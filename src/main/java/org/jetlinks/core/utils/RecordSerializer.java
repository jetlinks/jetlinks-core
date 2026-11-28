package org.jetlinks.core.utils;

import lombok.SneakyThrows;

import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.IdentityHashMap;

/**
 * 按组件名编码record。0x24为独立类型码，旧reader必须升级后才能接收此格式。
 * 单次SerializeUtils调用内保留record共享引用，循环回指明确拒绝。
 */
final class RecordSerializer implements SerializeUtils.Serializer {
    static final int CODE = 0x24;
    static final RecordSerializer INSTANCE = new RecordSerializer();
    private static final ThreadLocal<Scope> SCOPES = ThreadLocal.withInitial(() -> new Scope(null));

    @Override
    public int getCode() {
        return CODE;
    }

    @Override
    public Class<?> getJavaType() {
        return Record.class;
    }

    @Override
    @SneakyThrows
    public void serialize(Object value, ObjectOutput output) {
        Scope scope = enter(output);
        try {
            if (scope.written == null) {
                scope.written = new IdentityHashMap<>();
            }
            Integer previous = scope.written.get(value);
            if (previous != null) {
                if (previous < 0) {
                    throw new InvalidObjectException("Cyclic record reference: " + value.getClass().getName());
                }
                output.writeInt(-previous);
                return;
            }
            int identity = scope.written.size() + 1;
            scope.written.put(value, -identity);
            RecordMetadata metadata = RecordMetadata.of(value.getClass());
            output.writeInt(identity);
            output.writeUTF(value.getClass().getName());
            output.writeInt(metadata.size());
            for (int index = 0; index < metadata.size(); index++) {
                output.writeUTF(metadata.name(index));
                SerializeUtils.writeObject(metadata.value(value, index), output);
            }
            scope.written.put(value, identity);
        } finally {
            scope.close();
        }
    }

    @Override
    @SneakyThrows
    public Object deserialize(ObjectInput input) {
        Scope scope = enter(input);
        try {
            if (scope.read == null) {
                scope.read = new ArrayList<>();
            }
            int identity = input.readInt();
            if (identity < 0) {
                long index = -(long) identity - 1;
                if (index >= scope.read.size() || scope.read.get((int) index) == null) {
                    throw new StreamCorruptedException("Unknown or cyclic record reference: " + identity);
                }
                return scope.read.get((int) index);
            }
            if (identity != scope.read.size() + 1) {
                throw new StreamCorruptedException("Invalid record identity: " + identity);
            }
            String typeName = input.readUTF();
            Class<?> type = SerializeUtils.getClass(typeName);
            if (!type.isRecord()) {
                throw new InvalidObjectException("Not a record: " + typeName);
            }
            RecordMetadata metadata = RecordMetadata.of(type);
            int count = input.readInt();
            if (count < 0) {
                throw new StreamCorruptedException("Negative record component count: " + count);
            }
            scope.read.add(null);
            Object[] values = metadata.arguments();
            boolean[] assigned = new boolean[metadata.size()];
            for (int field = 0; field < count; field++) {
                String name = input.readUTF();
                Object value = SerializeUtils.readObject(input);
                Integer index = metadata.index(name);
                if (index != null) {
                    if (assigned[index]) {
                        throw new StreamCorruptedException("Duplicate record component: " + name);
                    }
                    assigned[index] = true;
                    values[index] = value;
                }
            }
            Object value = metadata.newInstance(values);
            scope.read.set(identity - 1, value);
            return value;
        } finally {
            scope.close();
        }
    }

    static Scope enter(Object stream) {
        Scope scope = SCOPES.get();
        if (scope.depth != 0 && scope.stream != stream) {
            scope = new Scope(scope);
            SCOPES.set(scope);
        }
        scope.stream = stream;
        scope.depth++;
        return scope;
    }

    static final class Scope {
        private final Scope parent;
        private Object stream;
        private int depth;
        private IdentityHashMap<Object, Integer> written;
        private ArrayList<Object> read;

        private Scope(Scope parent) {
            this.parent = parent;
        }

        void close() {
            if (--depth == 0) {
                // 仅保留空scope；失败、嵌套流及线程复用都不持有上一帧对象。
                stream = null;
                written = null;
                read = null;
                if (parent != null) {
                    SCOPES.set(parent);
                }
            }
        }
    }
}
