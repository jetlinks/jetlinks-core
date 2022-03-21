package org.jetlinks.core.trace;

import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.*;
import reactor.util.context.ContextView;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
class TraceFlux<T> extends FluxOperator<T, T> {
    private final String spanName;
    private final Tracer tracer;
    private final BiConsumer<Span, T> onNext;
    private final Consumer<SpanBuilder> builderConsumer;

    TraceFlux(Flux<? extends T> source,
              String name,
              Tracer tracer,
              BiConsumer<Span, T> onNext,
              Consumer<SpanBuilder> builderConsumer) {
        super(source);
        this.spanName = name;
        this.tracer = tracer;
        this.onNext = onNext;
        this.builderConsumer = builderConsumer;
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
        try {
            SpanBuilder builder = tracer.spanBuilder(spanName);
            ContextView context = actual.currentContext();

            Context ctx = context
                    .<Context>getOrEmpty(Context.class)
                    .orElseGet(Context::root);

            if (null != builderConsumer) {
                this.builderConsumer.accept(builder);
            }

            Span span = builder
                    .setParent(ctx)
                    .startSpan();

            TraceMonoSubscriber<T> subscriber = new TraceMonoSubscriber<>(actual, span, onNext);

            this.source
                    .contextWrite(reactor.util.context.Context
                                          .of(context)
                                          .put(SpanContext.class, span.getSpanContext())
                                          .put(Context.class, span.storeInContext(ctx)))
                    .subscribe(subscriber);

        } catch (Throwable e) {
            actual.onError(e);
        }

    }


    @AllArgsConstructor
    static class TraceMonoSubscriber<T> extends BaseSubscriber<T> {
        private final CoreSubscriber<? super T> actual;
        private Span span;
        private BiConsumer<Span, T> onNext;

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
            span.addEvent("cancel");
        }

        @Override
        protected void hookOnNext(@Nonnull T value) {
            if (null != onNext) {
                onNext.accept(span, value);
            }
            actual.onNext(value);
        }

        @Override
        protected void hookOnComplete() {
            span.setStatus(StatusCode.OK);
            actual.onComplete();
        }

    }
}