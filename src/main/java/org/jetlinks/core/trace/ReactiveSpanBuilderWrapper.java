package org.jetlinks.core.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import lombok.AllArgsConstructor;
import org.jetlinks.core.Lazy;
import org.jetlinks.core.LazyConverter;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor
public class ReactiveSpanBuilderWrapper implements ReactiveSpanBuilder {
    private final SpanBuilder spanBuilder;

    @Override
    @SuppressWarnings("all")
    public <T> ReactiveSpanBuilder setAttributeLazy(AttributeKey<T> key, Supplier<T> lazyValue) {
        spanBuilder.setAttribute((AttributeKey) key, (lazyValue instanceof Lazy ? lazyValue : Lazy.of(lazyValue)));
        return this;
    }

    @Override
    @SuppressWarnings("all")
    public <V, T> ReactiveSpanBuilder setAttributeLazy(AttributeKey<T> key, V value, Function<V, T> lazyValue) {
        spanBuilder.setAttribute((AttributeKey) key,
                                 (lazyValue instanceof LazyConverter ? lazyValue :
                                     LazyConverter.of(value, lazyValue)));
        return this;
    }

    @Override
    public ReactiveSpanBuilder setParent(@Nonnull Context context) {
        spanBuilder.setParent(context);
        return this;
    }

    @Override
    public ReactiveSpanBuilder setNoParent() {
        spanBuilder.setNoParent();
        return this;
    }

    @Override
    public ReactiveSpanBuilder addLink(@Nonnull SpanContext spanContext) {
        spanBuilder.addLink(spanContext);
        return this;
    }

    @Override
    public ReactiveSpanBuilder addLink(@Nonnull SpanContext spanContext, @Nonnull Attributes attributes) {
        spanBuilder.addLink(spanContext, attributes);
        return this;
    }

    @Override
    public ReactiveSpanBuilder setAttribute(@Nonnull String key, @Nonnull String value) {
        spanBuilder.setAttribute(key, value);
        return this;
    }

    @Override
    public ReactiveSpanBuilder setAttribute(@Nonnull String key, long value) {
        spanBuilder.setAttribute(key, value);
        return this;
    }

    @Override
    public ReactiveSpanBuilder setAttribute(@Nonnull String key, double value) {
        spanBuilder.setAttribute(key, value);
        return this;
    }

    @Override
    public ReactiveSpanBuilder setAttribute(@Nonnull String key, boolean value) {
        spanBuilder.setAttribute(key, value);
        return this;
    }

    @Override
    public <T> ReactiveSpanBuilder setAttribute(@Nonnull AttributeKey<T> key, @Nonnull T value) {
        spanBuilder.setAttribute(key, value);
        return this;
    }

    @Override
    public ReactiveSpanBuilder setSpanKind(@Nonnull SpanKind spanKind) {
        spanBuilder.setSpanKind(spanKind);
        return this;
    }

    @Override
    public ReactiveSpanBuilder setStartTimestamp(long startTimestamp, @Nonnull TimeUnit unit) {
        spanBuilder.setStartTimestamp(startTimestamp, unit);
        return this;
    }

    @Override
    public ReactiveSpanBuilder setStartTimestamp(@Nonnull Instant startTimestamp) {
        spanBuilder.setStartTimestamp(startTimestamp);
        return this;
    }

    @Override
    public ReactiveSpanBuilder setAllAttributes(@Nonnull Attributes attributes) {
        spanBuilder.setAllAttributes(attributes);
        return this;
    }

    @Override
    public Span startSpan() {
        return spanBuilder.startSpan();
    }
}
