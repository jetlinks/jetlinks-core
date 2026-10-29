package org.jetlinks.core.monitor.recorder;

import org.jetlinks.core.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.util.context.ContextView;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 组合原生 action 生命周期，而不是将普通 ActionRecord 回放到不同子类型。
 */
final class CompositeRecorder implements Recorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeRecorder.class);
    private final List<Recorder> recorders;
    private final CompositeAction<?> parent;

    private CompositeRecorder(List<Recorder> recorders) {
        this(recorders, null);
    }

    private CompositeRecorder(List<Recorder> recorders, CompositeAction<?> parent) {
        this.recorders = List.copyOf(recorders);
        this.parent = parent;
    }

    static Recorder withParent(Recorder recorder, ActionRecorder<?> parent) {
        Objects.requireNonNull(parent, "parent");
        Recorder selected = create(Objects.requireNonNull(recorder, "recorder"));
        if (selected instanceof CompositeRecorder composite && parent instanceof CompositeAction<?> action) {
            return new CompositeRecorder(composite.recorders, action);
        }
        return selected;
    }

    static Recorder create(Recorder... sources) {
        Objects.requireNonNull(sources, "recorders");
        List<Recorder> flattened = new ArrayList<>();
        IdentityHashMap<Recorder, Boolean> seen = new IdentityHashMap<>();
        for (Recorder recorder : sources) {
            flatten(Objects.requireNonNull(recorder, "recorder"), flattened, seen);
        }
        return flattened.isEmpty() ? Recorder.noop() : new CompositeRecorder(flattened);
    }

    private static void flatten(Recorder recorder, List<Recorder> result, IdentityHashMap<Recorder, Boolean> seen) {
        if (recorder == Recorder.noop() || seen.put(recorder, Boolean.TRUE) != null) {
            return;
        }
        if (recorder instanceof CompositeRecorder composite && composite.parent == null) {
            for (Recorder source : composite.recorders) {
                flatten(source, result, seen);
            }
        } else {
            result.add(recorder);
        }
    }

    @Override
    public <E> ActionRecorder<E> action(CharSequence name) {
        List<Target<E>> targets = new ArrayList<>(recorders.size());
        for (Recorder recorder : recorders) {
            try {
                Target<?> ancestor = parent == null ? null : parent.target(recorder);
                ActionRecorder<E> action = Objects.requireNonNull(
                    ancestor == null ? recorder.action(name) : ancestor.child(name), "action");
                if (action != ActionRecorder.<E>noop()) {
                    targets.add(new Target<>(recorder, action));
                }
            } catch (Throwable error) {
                report(error);
            }
        }
        return targets.isEmpty() ? ActionRecorder.noop() : new CompositeAction<>(targets);
    }

    @Override
    public void record(ActionRecord record) {
        if (record == null || record.getAction() == null) {
            return;
        }
        for (Recorder recorder : recorders) {
            try {
                // 保留调用方原记录类型；此入口不是 observe 的组合实现路径。
                recorder.record(record);
            } catch (Throwable error) {
                report(error);
            }
        }
    }

    private static void report(Throwable error) {
        Exceptions.throwIfFatal(error);
        LOGGER.warn("Monitor recorder callback failed", error);
    }

    private static final class Target<E> {
        private final Recorder owner;
        private final ActionRecorder<E> action;
        private volatile boolean failed;

        private Target(Recorder owner, ActionRecorder<E> action) {
            this.owner = owner;
            this.action = action;
        }

        private void accept(Consumer<ActionRecorder<E>> callback) {
            if (!failed) {
                try {
                    callback.accept(action);
                } catch (Throwable error) {
                    failed = true;
                    report(error);
                }
            }
        }

        private <T> ActionRecorder<T> child(CharSequence name) {
            if (failed) {
                return ActionRecorder.noop();
            }
            try {
                return Objects.requireNonNull(action.child(name), "child");
            } catch (Throwable error) {
                failed = true;
                report(error);
                return ActionRecorder.noop();
            }
        }
    }

    private static final class CompositeAction<E> extends AtomicBoolean implements ActionRecorder<E> {
        private final List<Target<E>> targets;
        private final AtomicBoolean started = new AtomicBoolean();

        private CompositeAction(List<Target<E>> targets) {
            this.targets = List.copyOf(targets);
        }

        private Target<?> target(Recorder recorder) {
            for (Target<E> target : targets) {
                if (target.owner == recorder) {
                    return target;
                }
            }
            return null;
        }

        private CompositeAction<E> each(Consumer<ActionRecorder<E>> callback) {
            for (Target<E> target : targets) {
                target.accept(callback);
            }
            return this;
        }

        private <V> ActionRecorder<E> supplied(Supplier<V> supplier, Consumer<V> writer) {
            if (targets.stream().noneMatch(target -> !target.failed)) {
                return this;
            }
            try {
                // 原 action 的 Supplier 可能立即求值，在分发前只求一次。
                writer.accept(supplier.get());
            } catch (Throwable error) {
                report(error);
            }
            return this;
        }

        @Override
        public ActionRecorder<E> tag(String key, Object value) {
            return each(action -> action.tag(key, value));
        }

        @Override
        public <V> ActionRecorder<E> tag(Key<V> key, V value) {
            return each(action -> action.tag(key, value));
        }

        @Override
        public <V> ActionRecorder<E> tag(Key<V> key, Supplier<V> value) {
            return supplied(value, resolved -> tag(key, resolved));
        }

        @Override
        public ActionRecorder<E> tags(Map<String, Object> tags) {
            return each(action -> action.tags(tags));
        }

        @Override
        public ActionRecorder<E> attribute(String key, Object value) {
            return each(action -> action.attribute(key, value));
        }

        @Override
        public <V> ActionRecorder<E> attribute(Key<V> key, V value) {
            return each(action -> action.attribute(key, value));
        }

        @Override
        public <V> ActionRecorder<E> attribute(Key<V> key, Supplier<V> value) {
            return supplied(value, resolved -> attribute(key, resolved));
        }

        @Override
        public ActionRecorder<E> attributes(Map<String, Object> attributes) {
            return each(action -> action.attributes(attributes));
        }

        @Override
        public ActionRecorder<E> error(Throwable error) {
            return compareAndSet(false, true) ? each(action -> action.error(error)) : this;
        }

        @Override
        public ActionRecorder<E> cancel() {
            return compareAndSet(false, true) ? each(ActionRecorder::cancel) : this;
        }

        @Override
        public ActionRecorder<E> complete() {
            return compareAndSet(false, true) ? each(ActionRecorder::complete) : this;
        }

        @Override
        public ActionRecorder<E> value(E value) {
            return get() ? this : each(action -> action.value(value));
        }

        @Override
        public ActionRecorder<E> valueConverter(Function<E, Object> converter) {
            return each(action -> action.valueConverter(converter));
        }

        @Override
        public ActionRecorder<E> start(ContextView context) {
            return started.compareAndSet(false, true) ? each(action -> action.start(context)) : this;
        }

        @Override
        public <T> ActionRecorder<T> child(CharSequence name) {
            List<Target<T>> children = new ArrayList<>(targets.size());
            for (Target<E> target : targets) {
                ActionRecorder<T> child = target.child(name);
                if (child != ActionRecorder.<T>noop()) {
                    children.add(new Target<>(target.owner, child));
                }
            }
            return children.isEmpty() ? ActionRecorder.noop() : new CompositeAction<>(children);
        }
    }
}
