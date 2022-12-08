package org.jetlinks.core.trace;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import lombok.extern.slf4j.Slf4j;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;
import reactor.util.context.ContextView;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * @param <T>
 */
@Slf4j
public class TraceMono<T> extends MonoOperator<T, T> {
    private final String spanName;
    private final Tracer tracer;
    private final BiConsumer<Span, T> onNext;
    private final BiConsumer<Span, Long> onComplete;
    private final BiConsumer<ContextView, SpanBuilder> onSubscription;

    public static <T> TraceMono<T> trace(Mono<T> source) {
        return new TraceMono<>(source,
                               null,
                               null,
                               null,
                               null,
                               null);
    }

    TraceMono(Mono<? extends T> source,
              String name,
              Tracer tracer,
              BiConsumer<Span, T> onNext,
              BiConsumer<Span, Long> onComplete,
              BiConsumer<ContextView, SpanBuilder> builderConsumer) {
        super(source);
        this.spanName = name == null ? this.name() : name;
        this.tracer = tracer == null ? TraceHolder.telemetry().getTracer(TraceHolder.appName()) : tracer;
        this.onNext = onNext;
        this.onSubscription = builderConsumer;
        this.onComplete = onComplete;
    }

    public TraceMono<T> spanName(String spanName) {
        return new TraceMono<>(this.source,
                               spanName,
                               this.tracer,
                               this.onNext,
                               this.onComplete,
                               this.onSubscription);
    }

    public TraceMono<T> scopeName(String scopeName) {
        return new TraceMono<>(this.source,
                               this.spanName,
                               TraceHolder.telemetry().getTracer(scopeName),
                               this.onNext,
                               this.onComplete,
                               this.onSubscription);
    }

    public TraceMono<T> scopeName(String scopeName, String scopeVersion) {
        return new TraceMono<>(this.source,
                               this.spanName,
                               TraceHolder.telemetry().getTracer(scopeName, scopeVersion),
                               this.onNext,
                               this.onComplete,
                               this.onSubscription);
    }

    public TraceMono<T> onNext(BiConsumer<Span, T> onNext) {
        if (this.onNext != null) {
            onNext = this.onNext.andThen(onNext);
        }
        return new TraceMono<>(this.source, this.spanName, this.tracer, onNext, this.onComplete, this.onSubscription);
    }

    public TraceMono<T> onComplete(BiConsumer<Span, Long> onComplete) {
        if (this.onComplete != null) {
            onComplete = this.onComplete.andThen(onComplete);
        }
        return new TraceMono<>(this.source, this.spanName, this.tracer, this.onNext, onComplete, this.onSubscription);
    }

    public TraceMono<T> onSubscription(BiConsumer<ContextView, SpanBuilder> onSubscription) {
        if (this.onSubscription != null) {
            onSubscription = this.onSubscription.andThen(onSubscription);
        }
        return new TraceMono<>(this.source, this.spanName, this.tracer, this.onNext, this.onComplete, onSubscription);
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
        try {
            SpanBuilder builder = tracer.spanBuilder(spanName);
            ContextView context = actual.currentContext();

            Context ctx = context
                    .<Context>getOrEmpty(Context.class)
                    .orElseGet(Context::root);

            if (null != onSubscription) {
                this.onSubscription.accept(context, builder);
            }

            Span span = builder
                    .setStartTimestamp(System.nanoTime(), TimeUnit.NANOSECONDS)
                    .setParent(ctx)
                    .startSpan();

            this.source.subscribe(new TraceSubscriber<>(actual, span, onNext, onComplete, ctx));

        } catch (Throwable e) {
            actual.onError(e);
        }

    }

}