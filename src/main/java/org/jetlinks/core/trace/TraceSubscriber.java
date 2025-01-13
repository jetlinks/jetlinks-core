package org.jetlinks.core.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import org.jetlinks.core.Lazy;
import org.jetlinks.core.LazyConverter;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.SignalType;
import reactor.function.Consumer3;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

class TraceSubscriber<T> extends BaseSubscriber<T> implements ReactiveSpan {

    final static AttributeKey<Long> count = AttributeKey.longKey("flux-next-count");

    @SuppressWarnings("all")
    final static AtomicLongFieldUpdater<TraceSubscriber> NEXT_COUNT = AtomicLongFieldUpdater
        .newUpdater(TraceSubscriber.class, "nextCount");

    private final CoreSubscriber<? super T> actual;
    private final Span span;
    private final Consumer3<ContextView, ReactiveSpan, T> onNext;
    private final Consumer3<ContextView, ReactiveSpan, Long> onComplete;
    private final BiConsumer<ContextView, Throwable> onError;

    private volatile long nextCount;
    private volatile boolean stateSet;
    private final long startWithNanos;
    private final Instant startWith;
    private final Context context;

    public TraceSubscriber(Instant startWith,
                           CoreSubscriber<? super T> actual,
                           Span span,
                           Consumer3<ContextView, ReactiveSpan, T> onNext,
                           Consumer3<ContextView, ReactiveSpan, Long> onComplete,
                           BiConsumer<ContextView, Throwable> onError,
                           io.opentelemetry.context.Context ctx) {
        this.actual = actual;
        this.span = span;
        this.onNext = onNext;
        this.onComplete = onComplete;
        this.onError = onError;
        this.startWithNanos = System.nanoTime();
        this.startWith = startWith;
        this.context = reactor.util.context.Context
            .of(actual.currentContext())
            .put(SpanContext.class, span.getSpanContext())
            .put(io.opentelemetry.context.Context.class, span.storeInContext(ctx));
    }

    @Override
    protected void hookOnSubscribe(@Nonnull Subscription subscription) {
        try (Scope ignored = span.makeCurrent()) {
            actual.onSubscribe(this);
        }
    }

    @Override
    protected void hookOnError(@Nonnull Throwable throwable) {
        span.setStatus(StatusCode.ERROR);
        if (onError != null) {
            onError.accept(context, throwable);
        } else {
            span.recordException(throwable);
        }
        try (Scope ignored = span.makeCurrent()) {
            actual.onError(throwable);
        }
    }

    @Override
    @Nonnull
    public Context currentContext() {
        return context;
    }

    @Override
    protected void hookFinally(@Nonnull SignalType type) {

        try (Scope ignored = context
            .get(io.opentelemetry.context.Context.class)
            .makeCurrent()) {
            span.end(startWith.plusNanos(System.nanoTime() - startWithNanos));
        }

    }

    @Override
    protected void hookOnCancel() {
        span.setAttribute(count, nextCount);
        if (nextCount > 0) {
            span.setStatus(StatusCode.OK, "cancel");
        } else if (!stateSet) {
            span.setStatus(StatusCode.ERROR, "cancel");
        }
    }

    @Override
    protected void hookOnNext(@Nonnull T value) {
        if (null != onNext) {
            onNext.accept(context, this, value);
        }
        stateSet = true;
        NEXT_COUNT.incrementAndGet(this);
        try (Scope ignored = span.makeCurrent()) {
            actual.onNext(value);
        }
    }

    @Override
    protected void hookOnComplete() {
        if (onComplete != null) {
            onComplete.accept(context, this, nextCount);
        }
        span.setAttribute(count, nextCount);
        if (!stateSet) {
            span.setStatus(StatusCode.OK);
        }
        stateSet = true;
        try (Scope ignored = span.makeCurrent()) {
            actual.onComplete();
        }
    }

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
        stateSet = true;
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