package org.jetlinks.core.monitor.recorder;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.Key;
import reactor.util.context.ContextView;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractActionRecorder<E> extends AtomicBoolean implements ActionRecorder<E> {

    protected final ActionRecord record;
    private long startWithNanos;
    private Function<E, Object> converter;

    public AbstractActionRecorder(CharSequence name) {
        this(name, null);
    }

    public AbstractActionRecorder(CharSequence name, String parentId) {
        this.record = create(name, parentId);
    }

    protected ActionRecord newRecord() {
        return new ActionRecord();
    }

    protected ActionRecord create(CharSequence name, String parentId) {
        ActionRecord _r = newRecord();
        _r.setId(IDGenerator.RANDOM.generate());
        _r.setParentId(parentId);
        _r.setAction(name);
        return _r;
    }

    protected abstract void handle(ActionRecord record);

    @Override
    public ActionRecorder<E> tag(String tag, Object value) {
        record.withTag(tag, value);
        return this;
    }

    @Override
    @SuppressWarnings("all")
    public <V> ActionRecorder<E> tag(Key<V> key, V value) {
        record
            .writableTags()
            .compute(key.getKey(), (k, old) -> {
                if (old == null) {
                    return value;
                }
                return key.apply((V) old, value);
            });
        return this;
    }

    @Override
    public <V> ActionRecorder<E> tag(Key<V> key, Supplier<V> value) {
        return tag(key, value.get());
    }

    @Override
    public ActionRecorder<E> tags(Map<String, Object> tags) {
        record.withTags(tags);
        return this;
    }

    @Override
    public ActionRecorder<E> attributes(Map<String, Object> data) {
        record.withAttributes(data);
        return this;
    }

    @Override
    public ActionRecorder<E> attribute(String tag, Object value) {
        record.withAttributes(tag, value);
        return this;
    }

    @Override
    @SuppressWarnings("all")
    public <V> ActionRecorder<E> attribute(Key<V> key, V value) {
        record
            .writableAttributes()
            .compute(key.getKey(), (k, old) -> {
                if (old == null) {
                    return value;
                }
                return key.apply((V) old, value);
            });
        return this;
    }

    @Override
    public <V> ActionRecorder<E> attribute(Key<V> key, Supplier<V> value) {
        return attribute(key, value.get());
    }

    @Override
    public ActionRecorder<E> error(Throwable error) {
        if (compareAndSet(false, true)) {
            record.withError(error);
            record.end(System.nanoTime() - startWithNanos);
            handle(record);
        }
        return this;
    }

    @Override
    public ActionRecorder<E> cancel() {
        if (compareAndSet(false, true)) {
            record.setCancel(true);
            record.end(System.nanoTime() - startWithNanos);
            handle(record);
        }

        return this;
    }

    @Override
    public ActionRecorder<E> complete() {
        if (compareAndSet(false, true)) {
            record.end(System.nanoTime() - startWithNanos);
            handle(record);
        }
        return this;
    }

    @Override
    public ActionRecorder<E> value(E value) {
        record.withValue(value);
        return this;
    }

    @Override
    public ActionRecorder<E> valueConverter(Function<E, Object> converter) {
        this.converter = converter;
        return this;
    }

    @Override
    public ActionRecorder<E> start(ContextView context) {
        record.setTimestamp(System.currentTimeMillis());
        startWithNanos = System.nanoTime();

        Context ctx = context.getOrDefault(Context.class,Context.current());
        SpanContext spanContext = Span.fromContext(ctx).getSpanContext();
        if (spanContext.isValid()) {
            record.setTraceId(spanContext.getTraceId());
            record.setSpanId(spanContext.getSpanId());
        }
        return this;
    }
}
