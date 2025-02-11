package org.jetlinks.core.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import lombok.AllArgsConstructor;
import org.jetlinks.core.Lazy;
import org.jetlinks.core.LazyConverter;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor
public class ReactiveSpanWrapper implements ReactiveSpan {
    private final Span span;

    @Override
    @SuppressWarnings("all")
    public <T> ReactiveSpan setAttributeLazy(AttributeKey<T> key, Supplier<T> lazyValue) {
        span.setAttribute((AttributeKey) key, (lazyValue instanceof Lazy ? lazyValue : Lazy.of(lazyValue)));
        return this;
    }

    @Override
    @SuppressWarnings("all")
    public <V, T> ReactiveSpan setAttributeLazy(AttributeKey<T> key, V value, Function<V, T> lazyValue) {
        span.setAttribute((AttributeKey) key,
                          (lazyValue instanceof LazyConverter ? lazyValue :
                                  LazyConverter.of(value, lazyValue)));
        return this;
    }

    @Override
    public ReactiveSpan setAttribute(@Nonnull String key, double value) {
        span.setAttribute(key, value);
        return this;
    }

    @Override
    public ReactiveSpan setAttribute(@Nonnull String key, long value) {
        span.setAttribute(key, value);
        return this;
    }

    @Override
    public <R> ReactiveSpan setAttribute(@Nonnull AttributeKey<R> key, @Nonnull R value) {
        span.setAttribute(key, value);
        return this;
    }

    @Override
    public ReactiveSpan setAttribute(@Nonnull String key, @Nonnull String value) {
        span.setAttribute(key, value);
        return this;
    }

    @Override
    public ReactiveSpan setAttribute(@Nonnull String key, boolean value) {
        span.setAttribute(key, value);
        return this;
    }

    @Override
    public ReactiveSpan setAttribute(@Nonnull AttributeKey<Long> key, int value) {
        span.setAttribute(key, value);
        return this;
    }

    @Override
    public ReactiveSpan setStatus(@Nonnull StatusCode statusCode) {
        span.setStatus(statusCode);
        return this;
    }

    @Override
    public Span setAllAttributes(@Nonnull Attributes attributes) {
        span.setAllAttributes(attributes);
        return this;
    }

    @Override
    public ReactiveSpan addEvent(@Nonnull String name, @Nonnull Attributes attributes) {
        span.addEvent(name, attributes);
        return this;
    }

    @Override
    public ReactiveSpan addEvent(@Nonnull String name, @Nonnull Attributes attributes, long timestamp, @Nonnull TimeUnit unit) {
        span.addEvent(name, attributes, timestamp, unit);
        return this;
    }

    @Override
    public ReactiveSpan setStatus(@Nonnull StatusCode statusCode, @Nonnull String description) {
        span.setStatus(statusCode, description);
        return this;
    }

    @Override
    public ReactiveSpan recordException(@Nonnull Throwable exception, @Nonnull Attributes additionalAttributes) {
        span.recordException(exception, additionalAttributes);
        return this;
    }

    @Override
    public ReactiveSpan updateName(@Nonnull String name) {
        span.updateName(name);
        return this;
    }

    @Override
    public void end() {
        //do nothing
    }

    @Override
    public void end(long timestamp, @Nonnull TimeUnit unit) {
        //do nothing
    }

    @Override
    public SpanContext getSpanContext() {
        return span.getSpanContext();
    }

    @Override
    public boolean isRecording() {
        return span.isRecording();
    }

    @Override
    public io.opentelemetry.context.Context storeInContext(@Nonnull io.opentelemetry.context.Context context) {
        return span.storeInContext(context);
    }
}
