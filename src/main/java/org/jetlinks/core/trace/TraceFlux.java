package org.jetlinks.core.trace;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;
import reactor.function.Consumer3;
import reactor.util.context.ContextView;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Slf4j
public class TraceFlux<T> extends FluxOperator<T, T> {
    private final String spanName;
    private final Tracer tracer;
    private final Consumer3<ContextView, ReactiveSpan, T> onNext;
    private final Consumer3<ContextView, ReactiveSpan, Long> onComplete;
    private final BiConsumer<ContextView, ReactiveSpanBuilder> onSubscription;
    private final BiConsumer<ContextView, Throwable> onError;

    public static <T> TraceFlux<T> trace(Publisher<T> source) {

        return new TraceFlux<>(Flux.from(source),
                               null,
                               null,
                               null,
                               null,
                               null,
                               null);
    }

    TraceFlux(Flux<? extends T> source,
              String name,
              Tracer tracer,
              Consumer3<ContextView, ReactiveSpan, T> onNext,
              Consumer3<ContextView, ReactiveSpan, Long> onComplete,
              BiConsumer<ContextView,ReactiveSpanBuilder> builderConsumer,
              BiConsumer<ContextView, Throwable> onError) {
        super(source);
        this.spanName = name == null ? this.name() : name;
        this.tracer = tracer == null ? TraceHolder.telemetry().getTracer(TraceHolder.appName()) : tracer;
        this.onNext = onNext;
        this.onSubscription = builderConsumer;
        this.onComplete = onComplete;
        this.onError = onError;
    }

    public TraceFlux<T> onNext(BiConsumer<ReactiveSpan, T> onNext) {
        return onNext((ctx, span, r) -> onNext.accept(span, r));
    }

    public TraceFlux<T> onNext(Consumer3<ContextView, ReactiveSpan, T> callback) {
        Consumer3<ContextView, ReactiveSpan, T> that = this.onNext;

        Consumer3<ContextView, ReactiveSpan, T> onNext = that == null
                ?
                callback
                :
                (contextView, span, r) -> {
                    that.accept(contextView, span, r);
                    callback.accept(contextView, span, r);
                };
        return new TraceFlux<>(this.source, this.spanName, this.tracer, onNext, this.onComplete, this.onSubscription, this.onError);
    }

    public TraceFlux<T> onComplete(Consumer3<ContextView, ReactiveSpan, Long> callback) {
        Consumer3<ContextView, ReactiveSpan, Long> that = this.onComplete;

        Consumer3<ContextView, ReactiveSpan, Long> onComplete = that == null
                ?
                callback
                :
                (contextView, span, r) -> {
                    that.accept(contextView, span, r);
                    callback.accept(contextView, span, r);
                };

        return new TraceFlux<>(this.source,
                               this.spanName,
                               this.tracer,
                               this.onNext,
                               onComplete,
                               this.onSubscription,
                               this.onError);
    }

    public TraceFlux<T> onComplete(BiConsumer<Span, Long> onComplete) {
        return onComplete((ctx, span, len) -> onComplete.accept(span, len));
    }

    public TraceFlux<T> spanName(String spanName) {
        return new TraceFlux<>(this.source,
                               spanName,
                               this.tracer,
                               this.onNext,
                               this.onComplete,
                               this.onSubscription,
                               this.onError);
    }

    public TraceFlux<T> scopeName(String scopeName) {
        return new TraceFlux<>(this.source,
                               this.spanName,
                               TraceHolder.telemetry().getTracer(scopeName),
                               this.onNext,
                               this.onComplete,
                               this.onSubscription,
                               this.onError);
    }

    public TraceFlux<T> onSubscription(BiConsumer<ContextView, ReactiveSpanBuilder> onSubscription) {
        if (this.onSubscription != null) {
            onSubscription = this.onSubscription.andThen(onSubscription);
        }
        return new TraceFlux<>(this.source, this.spanName, this.tracer, this.onNext, this.onComplete, onSubscription, onError);
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
        try {
            ReactiveSpanBuilder builder = new ReactiveSpanBuilderWrapper(tracer.spanBuilder(spanName));
            ContextView context = actual.currentContext();

            Context ctx = context
                    .<Context>getOrEmpty(Context.class)
                    .orElseGet(Context::root);

            if (null != onSubscription) {
                this.onSubscription.accept(context, builder);
            }

            Span span = builder
                    .setStartTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .setParent(ctx)
                    .startSpan();

            this.source
                    .subscribe(new TraceSubscriber<>(actual, span, onNext, onComplete, onError,ctx));

        } catch (Throwable e) {
            actual.onError(e);
        }

    }

}