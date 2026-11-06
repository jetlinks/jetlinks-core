package org.jetlinks.core.monitor.recorder;

import org.jetlinks.core.Key;
import reactor.core.Exceptions;
import reactor.util.context.ContextView;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 一次执行内的选择性记录器，只保留白名单元数据和局部操作关系。
 *
 * <p>工厂复用字段索引，实例不跨订阅共享。操作以局部序号关联，不分配完整 ActionRecord、
 * UUID、链路上下文或堆栈；业务结果只能经显式 tag/attribute 摘要进入记录。
 * 元数据接受不可变标量及深度不超过3的 List/Map 副本，不反射业务对象。</p>
 *
 * <p>所有写入和释放使用 CAS；快照中每个操作自身一致，跨操作一致性由调用方的执行屏障保证。
 * 本类不订阅业务、不执行 I/O、不管理历史留存或全局缓存。</p>
 *
 * @since 1.3.3
 * @see Recorder#combine(Recorder...)
 * @see org.jetlinks.core.monitor.observation.Observations
 */
public final class InMemoryRecorder implements Recorder, AutoCloseable {

    private static final int TRUNCATED = 1;
    private static final int CLOSED = 2;
    private static final int MAX_NAME_LENGTH = 128;
    // Updater 和数组访问器只创建一次，执行实例内只保留字段与原生数组。
    private static final AtomicIntegerFieldUpdater<InMemoryRecorder> SIZE =
        AtomicIntegerFieldUpdater.newUpdater(InMemoryRecorder.class, "size");
    private static final AtomicIntegerFieldUpdater<InMemoryRecorder> FLAGS =
        AtomicIntegerFieldUpdater.newUpdater(InMemoryRecorder.class, "flags");
    private static final AtomicLongFieldUpdater<InMemoryRecorder> USED =
        AtomicLongFieldUpdater.newUpdater(InMemoryRecorder.class, "used");
    private static final VarHandle ACTIONS = MethodHandles.arrayElementVarHandle(MemoryAction[].class);

    private final Selection selection;
    private final MemoryAction<?>[] actions;
    private volatile int size;
    private volatile int flags;
    // 高32位为字符数，低32位为值节点数；一次 CAS 同时接纳两个预算。
    private volatile long used;

    private InMemoryRecorder(Selection selection) {
        this.selection = selection;
        actions = new MemoryAction<?>[selection.limits.maxActions()];
    }

    /** 创建可跨执行复用的字段选择工厂；每次 get 返回独立接收端。 */
    public static Supplier<InMemoryRecorder> factory(Set<String> tags, Set<String> attributes) {
        return factory(tags, attributes, Limits.defaults());
    }

    /**
     * 预编译字段白名单；最多64个字段，字段名最长128字符。
     * 值预算按接纳的写入累计，覆盖及 CAS 重试不返还预算，不承诺精确的堆或编码字节大小。
     * 类型化 Key 的合并函数必须无副作用，CAS 竞争时可能重试。
     *
     * @param tags 保留的 tag 名称
     * @param attributes 保留的 attribute 名称
     * @param limits 单次执行的容量
     * @return 创建独立接收端的无状态工厂
     */
    public static Supplier<InMemoryRecorder> factory(Set<String> tags, Set<String> attributes, Limits limits) {
        Objects.requireNonNull(limits, "limits");
        Map<String, Integer> tagIndexes = indexes(tags, 0);
        Map<String, Integer> attributeIndexes = indexes(attributes, tagIndexes.size());
        if (tagIndexes.size() + attributeIndexes.size() > 64) {
            throw new IllegalArgumentException("at most 64 selected fields are supported");
        }
        Selection selection = new Selection(tagIndexes, attributeIndexes, limits);
        return () -> new InMemoryRecorder(selection);
    }

    private static Map<String, Integer> indexes(Set<String> names, int offset) {
        Objects.requireNonNull(names, "names");
        if (names.size() > 64) throw new IllegalArgumentException("at most 64 selected fields are supported");
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String name : names) {
            if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
                throw new IllegalArgumentException("selected field names must contain 1 to 128 characters");
            }
            result.put(name, offset + result.size());
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public <E> ActionRecorder<E> action(CharSequence action) {
        return create(action, -1);
    }

    private <E> ActionRecorder<E> create(CharSequence name, int parentId) {
        Objects.requireNonNull(name, "action");
        if (closed()) return ActionRecorder.noop();
        if (name.length() == 0 || name.length() > MAX_NAME_LENGTH) {
            truncate();
            return ActionRecorder.noop();
        }
        int index;
        do {
            index = size;
            if (index >= actions.length) {
                truncate();
                return ActionRecorder.noop();
            }
        } while (!SIZE.compareAndSet(this, index, index + 1));
        String actionName = name.toString();
        if (actionName.length() > MAX_NAME_LENGTH) { truncate(); return ActionRecorder.noop(); }
        MemoryAction<E> action = new MemoryAction<>(index, parentId, actionName);
        ACTIONS.setVolatile(actions, index, action);
        // close 可与已预留但尚未发布的操作并发；迟到发布者负责释放自己的状态。
        if (closed()) {
            action.frame = Frame.DISCARDED;
            ACTIONS.compareAndSet(actions, index, action, null);
            return ActionRecorder.noop();
        }
        return action;
    }

    /**
     * 返回独立且不可变的当前快照，不终止操作。局部 id 仅在本接收端内唯一，根 parentId 为-1。
     * 应在相关步骤完成后提取；仍在执行的操作会明确标记为 created/running。
     */
    public Snapshot snapshot() {
        List<ActionSnapshot> result = new ArrayList<>();
        for (int i = 0, count = size; i < count; i++) {
            MemoryAction<?> action = (MemoryAction<?>) ACTIONS.getVolatile(actions, i);
            if (action == null) continue;
            Frame frame = action.frame;
            if (frame == Frame.DISCARDED) continue;
            result.add(new ActionSnapshot(action.id, action.parentId, action.name, frame.status,
                frame.timestamp, frame.useNanos, fields(selection.tags, frame.values),
                fields(selection.attributes, frame.values), frame.errorType, frame.errorMessage));
        }
        return new Snapshot(result, (flags & TRUNCATED) != 0);
    }

    private static Map<String, Object> fields(Map<String, Integer> indexes, Object[] values) {
        if (values == null || indexes.isEmpty()) return Map.of();
        String firstName = null;
        Object firstValue = null;
        Map<String, Object> result = null;
        for (Map.Entry<String, Integer> entry : indexes.entrySet()) {
            Object value = values[entry.getValue()];
            if (value == null) continue;
            if (firstName == null) {
                firstName = entry.getKey();
                firstValue = value;
            } else {
                if (result == null) {
                    result = new LinkedHashMap<>();
                    result.put(firstName, firstValue);
                }
                result.put(entry.getKey(), value);
            }
        }
        // 多数步骤只输出一个摘要，避免为零/单字段分配哈希表和包装器。
        if (result != null) return Collections.unmodifiableMap(result);
        return firstName == null ? Map.of() : Map.of(firstName, firstValue);
    }

    /** 释放已保留的值并禁止后续写入；已提取的快照继续有效，不重置或复用本实例。 */
    @Override
    public void close() {
        if ((FLAGS.getAndUpdate(this, value -> value | CLOSED) & CLOSED) != 0) return;
        for (int i = 0; i < actions.length; i++) {
            MemoryAction<?> action = (MemoryAction<?>) ACTIONS.getAndSet(actions, i, null);
            if (action != null) action.frame = Frame.DISCARDED;
        }
    }

    private boolean closed() { return (flags & CLOSED) != 0; }
    private void truncate() {
        if ((flags & TRUNCATED) == 0) FLAGS.getAndUpdate(this, value -> value | TRUNCATED);
    }

    private boolean reserve(ValueCopy copy) {
        long previous, next;
        do {
            previous = used;
            int values = (int) previous + copy.values;
            int characters = (int) (previous >>> 32) + copy.characters;
            if (values > selection.limits.maxValues() || characters > selection.limits.maxCharacters()) return false;
            next = ((long) characters << 32) | values;
        } while (!USED.compareAndSet(this, previous, next));
        return true;
    }

    /** 元数据容量，字符数和节点数是保守接纳预算，不是序列化字节数或真实堆占用。 */
    public record Limits(int maxActions, int maxValues, int maxCharacters) {
        public Limits {
            if (maxActions < 1 || maxActions > 4096 || maxValues < 0 || maxValues > 1_000_000
                || maxCharacters < 0 || maxCharacters > 1_000_000) {
                throw new IllegalArgumentException("invalid in-memory recorder limits");
            }
        }
        public static Limits defaults() { return new Limits(32, 128, 4096); }
    }

    /** 操作生命周期；completed 只表示执行完成，不代表业务已持久化。 */
    public enum Status { created, running, completed, failed, cancelled }

    /** 有界执行快照；truncated 表示超限、类型不支持或提取失败造成了记录缺口。 */
    public record Snapshot(List<ActionSnapshot> actions, boolean truncated) {
        public Snapshot { actions = List.copyOf(actions); }
    }

    /** 不持有原操作、业务结果、上下文或 Throwable 的操作快照。 */
    public record ActionSnapshot(int id, int parentId, String action, Status status, long timestamp,
                                 long useNanos, Map<String, Object> tags, Map<String, Object> attributes,
                                 String errorType, String errorMessage) { }

    private record Selection(Map<String, Integer> tags, Map<String, Integer> attributes, Limits limits) {
        int fields() { return tags.size() + attributes.size(); }
    }

    private record Frame(Object[] values, Status status, long timestamp, long startNanos,
                         long useNanos, String errorType, String errorMessage) {
        static final Frame CREATED = new Frame(null, Status.created, 0, 0, 0, null, null);
        static final Frame DISCARDED = new Frame(null, Status.completed, 0, 0, 0, null, null);
        boolean ended() { return status.ordinal() >= Status.completed.ordinal(); }
    }

    private final class MemoryAction<E> implements ActionRecorder<E> {
        @SuppressWarnings("rawtypes")
        private static final AtomicReferenceFieldUpdater<MemoryAction, Frame> FRAME =
            AtomicReferenceFieldUpdater.newUpdater(MemoryAction.class, Frame.class, "frame");

        private final int id;
        private final int parentId;
        private final String name;
        private volatile Frame frame = Frame.CREATED;

        private MemoryAction(int id, int parentId, String name) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
        }

        private boolean writable() { return !closed() && !frame.ended(); }

        private boolean metadataWritable() {
            if (!writable()) return false;
            if ((int) used >= selection.limits.maxValues()) { truncate(); return false; }
            return true;
        }

        private <V> ActionRecorder<E> write(Map<String, Integer> indexes, String name, Key<V> key, V value) {
            Integer index = indexes.get(name);
            if (index == null || value == null || !metadataWritable()) return this;
            try {
                Frame previous;
                do {
                    previous = frame;
                    if (closed() || previous.ended()) return this;
                    Object selected = value;
                    Object old = previous.values == null ? null : previous.values[index];
                    if (old != null && key != null) selected = merge(key, old, value);
                    long budget = used;
                    ValueCopy copy = new ValueCopy(selection.limits.maxValues() - (int) budget,
                        selection.limits.maxCharacters() - (int) (budget >>> 32));
                    Object snapshot = copy.copy(selected, 0);
                    if (!reserve(copy)) throw Omitted.INSTANCE;
                    Object[] fields = previous.values == null ? new Object[selection.fields()] : previous.values.clone();
                    fields[index] = snapshot;
                    Frame next = new Frame(fields, previous.status, previous.timestamp, previous.startNanos,
                        previous.useNanos, previous.errorType, previous.errorMessage);
                    if (FRAME.compareAndSet(this, previous, next)) return this;
                } while (true);
            } catch (Throwable error) {
                Exceptions.throwIfFatal(error);
                truncate();
                return this;
            }
        }

        @SuppressWarnings("unchecked")
        private <V> Object merge(Key<V> key, Object old, V value) { return key.apply((V) old, value); }

        @Override
        public ActionRecorder<E> tag(String key, Object value) { return write(selection.tags, key, null, value); }
        @Override
        public <V> ActionRecorder<E> tag(Key<V> key, V value) { return write(selection.tags, key.getKey(), key, value); }
        @Override
        public <V> ActionRecorder<E> attribute(Key<V> key, V value) { return write(selection.attributes, key.getKey(), key, value); }
        @Override
        public ActionRecorder<E> attribute(String key, Object value) { return write(selection.attributes, key, null, value); }
        @Override
        public <V> ActionRecorder<E> tag(Key<V> key, Supplier<V> value) { return supplied(selection.tags, key, value); }
        @Override
        public <V> ActionRecorder<E> attribute(Key<V> key, Supplier<V> value) { return supplied(selection.attributes, key, value); }

        private <V> ActionRecorder<E> supplied(Map<String, Integer> indexes, Key<V> key, Supplier<V> supplier) {
            if (!indexes.containsKey(key.getKey()) || !metadataWritable()) return this;
            try { return write(indexes, key.getKey(), key, supplier.get()); }
            catch (Throwable error) { Exceptions.throwIfFatal(error); truncate(); return this; }
        }

        @Override
        public ActionRecorder<E> tags(Map<String, Object> values) { return bulk(selection.tags, values); }
        @Override
        public ActionRecorder<E> attributes(Map<String, Object> values) { return bulk(selection.attributes, values); }

        private ActionRecorder<E> bulk(Map<String, Integer> indexes, Map<String, Object> values) {
            if (values != null && writable()) {
                // 遍历预编译白名单，避免扫描调用方提供的无界元数据 Map。
                for (String key : indexes.keySet()) {
                    try { write(indexes, key, null, values.get(key)); }
                    catch (Throwable error) { Exceptions.throwIfFatal(error); truncate(); }
                }
            }
            return this;
        }

        @Override
        public ActionRecorder<E> start(ContextView context) {
            Frame previous;
            do {
                previous = frame;
                if (closed() || previous.status != Status.created) return this;
            } while (!FRAME.compareAndSet(this, previous, new Frame(previous.values, Status.running,
                System.currentTimeMillis(), System.nanoTime(), 0, null, null)));
            return this;
        }

        @Override
        public ActionRecorder<E> complete() { return finish(Status.completed, null); }
        @Override
        public ActionRecorder<E> cancel() { return finish(Status.cancelled, null); }
        @Override
        public ActionRecorder<E> error(Throwable error) { return finish(Status.failed, Objects.requireNonNull(error, "error")); }

        private ActionRecorder<E> finish(Status status, Throwable error) {
            if (!writable()) return this;
            String message = null;
            if (error != null) {
                try {
                    message = error.getMessage();
                    if (message != null && message.length() > 512) { message = message.substring(0, 512); truncate(); }
                } catch (Throwable failure) { Exceptions.throwIfFatal(failure); truncate(); }
            }
            Frame previous;
            do {
                previous = frame;
                if (closed() || previous.ended()) return this;
            } while (!FRAME.compareAndSet(this, previous, new Frame(previous.values, status, previous.timestamp,
                0, previous.status == Status.running ? System.nanoTime() - previous.startNanos : 0,
                error == null ? null : error.getClass().getName(), message)));
            return this;
        }

        @Override
        public <T> ActionRecorder<T> child(CharSequence action) { return create(action, id); }
        @Override
        public ActionRecorder<E> value(E value) { return this; }
        @Override
        public ActionRecorder<E> valueConverter(Function<E, Object> converter) { return this; }
    }

    /** 只复制可审计的值形状；不调用业务对象序列化、toString 或反射。 */
    private static final class ValueCopy {
        private final int maxValues;
        private final int maxCharacters;
        private int values;
        private int characters;

        ValueCopy(int maxValues, int maxCharacters) {
            this.maxValues = maxValues;
            this.maxCharacters = maxCharacters;
        }

        private void characters(int count) {
            if (count > maxCharacters - characters) throw Omitted.INSTANCE;
            characters += count;
        }

        private Object copy(Object value, int depth) {
            if (++values > maxValues) throw Omitted.INSTANCE;
            if (value == null || value instanceof Boolean) return value;
            if (value instanceof String text) { characters(text.length()); return text; }
            if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
                || value instanceof Float || value instanceof Double) {
                characters(24);
                return value;
            }
            if (value.getClass() == BigInteger.class) {
                BigInteger integer = (BigInteger) value;
                if (integer.bitLength() > 3L * (maxCharacters - characters)) throw Omitted.INSTANCE;
                characters(integer.toString().length());
                return integer;
            }
            if (value.getClass() == BigDecimal.class) {
                BigDecimal decimal = (BigDecimal) value;
                if (Math.abs((long) decimal.scale()) > maxCharacters || decimal.precision() > maxCharacters - characters) throw Omitted.INSTANCE;
                characters(decimal.toString().length());
                return decimal;
            }
            if (depth >= 3) throw Omitted.INSTANCE;
            if (value instanceof List<?> list) {
                if (list.size() > maxValues - values) throw Omitted.INSTANCE;
                List<Object> result = new ArrayList<>(list.size());
                for (Object element : list) result.add(copy(element, depth + 1));
                return Collections.unmodifiableList(result);
            }
            if (value instanceof Map<?, ?> map) {
                if (map.size() > maxValues - values) throw Omitted.INSTANCE;
                Map<String, Object> result = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) throw Omitted.INSTANCE;
                    characters(key.length());
                    result.put(key, copy(entry.getValue(), depth + 1));
                }
                return Collections.unmodifiableMap(result);
            }
            throw Omitted.INSTANCE;
        }
    }

    private static final class Omitted extends RuntimeException {
        private static final Omitted INSTANCE = new Omitted();
        private Omitted() { super(null, null, false, false); }
    }
}
