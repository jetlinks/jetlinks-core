package org.jetlinks.core.monitor.observation;

import org.jetlinks.core.Key;
import org.jetlinks.core.monitor.recorder.ActionRecorder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 一次回调的暂存；保留 Key 的泛型绑定，提交时仍走类型化 API，而不是 Map 覆盖。
 */
final class MetadataBuffer implements RecordMetadata {

    private static final int LIMIT = 64;

    private final List<Write<?>> writes = new ArrayList<>();
    private int elements;

    @Override
    public <V> void tag(Key<V> key, V value) {
        add(key, value, true);
    }

    @Override
    public <V> void attribute(Key<V> key, V value) {
        add(key, value, false);
    }

    private <V> void add(Key<V> key, V value, boolean tag) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.getKey(), "key name");
        if (key.getKey().startsWith("observation.")) {
            throw new IllegalArgumentException("observation.* is reserved");
        }
        if (value == null) {
            return;
        }
        int size = value instanceof Collection<?> collection ? collection.size() : 0;
        if (writes.size() >= LIMIT || size > LIMIT - elements) {
            throw new LimitExceeded();
        }
        elements += size;
        writes.add(new Write<>(key, value, tag));
    }

    void commit(ActionRecorder<?> action, Budget budget) {
        if (writes.size() > LIMIT - budget.writes || elements > LIMIT - budget.elements) {
            throw new LimitExceeded();
        }
        budget.writes += writes.size();
        budget.elements += elements;
        for (Write<?> write : writes) {
            write.apply(action);
        }
    }

    static final class Budget {
        private int writes;
        private int elements;
    }

    static final class LimitExceeded extends IllegalArgumentException {
        private LimitExceeded() {
            super("Observation metadata limit exceeded");
        }
    }

    private record Write<V>(Key<V> key, V value, boolean tag) {
        void apply(ActionRecorder<?> action) {
            if (tag) {
                action.tag(key, value);
            } else {
                action.attribute(key, value);
            }
        }
    }
}
