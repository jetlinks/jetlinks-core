package org.jetlinks.core.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.SignalType;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.BiConsumer;

class TraceSubscriber<T> extends BaseSubscriber<T> implements Span {

    final static AttributeKey<Long> count = AttributeKey.longKey("count");

    @SuppressWarnings("all")
    final static AtomicLongFieldUpdater<TraceSubscriber> NEXT_COUNT = AtomicLongFieldUpdater
            .newUpdater(TraceSubscriber.class,"nextCount");

    private final CoreSubscriber<? super T> actual;
    private final Span span;
    private final BiConsumer<Span, T> onNext;
    private final BiConsumer<Span, Long> onComplete;


    private volatile long nextCount;
    private volatile boolean stateSet;

    public TraceSubscriber(CoreSubscriber<? super T> actual,
                           Span span,
                           BiConsumer<Span, T> onNext,
                           BiConsumer<Span, Long> onComplete) {
        this.actual = actual;
        this.span = span;
        this.onNext = onNext;
        this.onComplete = onComplete;
    }

    @Override
    protected void hookOnSubscribe(@Nonnull Subscription subscription) {
        actual.onSubscribe(this);
    }

    @Override
    protected void hookOnError(@Nonnull Throwable throwable) {
        span.setStatus(StatusCode.ERROR);
        span.recordException(throwable);
        actual.onError(throwable);
    }

    @Override
    protected void hookFinally(@Nonnull SignalType type) {
        span.end();
    }

    @Override
    protected void hookOnCancel() {
        if (!stateSet) {
            span.setStatus(StatusCode.ERROR, "cancel");
        }
    }

    @Override
    protected void hookOnNext(@Nonnull T value) {
        if (null != onNext) {
            onNext.accept(this, value);
        }
        NEXT_COUNT.incrementAndGet(this);
        actual.onNext(value);
    }

    @Override
    protected void hookOnComplete() {
        if (onComplete != null) {
            onComplete.accept(this, nextCount);
        }
        span.setAttribute(count,nextCount);
        if (!stateSet) {
            span.setStatus(StatusCode.OK);
        }
        actual.onComplete();
    }

    @Override
    public <R> Span setAttribute(@Nonnull AttributeKey<R> key,@Nonnull R value) {
        span.setAttribute(key, value);
        return this;
    }

    @Override
    public Span addEvent(@Nonnull String name,@Nonnull Attributes attributes) {
        span.addEvent(name, attributes);
        return this;
    }

    @Override
    public Span addEvent(@Nonnull String name,@Nonnull Attributes attributes, long timestamp,@Nonnull TimeUnit unit) {
        span.addEvent(name, attributes, timestamp, unit);
        return this;
    }

    @Override
    public Span setStatus(@Nonnull StatusCode statusCode, @Nonnull String description) {
        stateSet = true;
        span.setStatus(statusCode, description);
        return this;
    }

    @Override
    public Span recordException(@Nonnull Throwable exception,@Nonnull Attributes additionalAttributes) {
        span.recordException(exception, additionalAttributes);
        return this;
    }

    @Override
    public Span updateName(@Nonnull String name) {
        span.updateName(name);
        return this;
    }

    @Override
    public void end() {
        //do nothing
    }

    @Override
    public void end(long timestamp,@Nonnull TimeUnit unit) {
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
}