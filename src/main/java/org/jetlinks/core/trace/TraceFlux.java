package org.jetlinks.core.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;
import reactor.core.publisher.SignalType;
import reactor.util.context.ContextView;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Slf4j
class TraceFlux<T> extends FluxOperator<T, T> {
    private final String spanName;
    private final Tracer tracer;
    private final BiConsumer<Span, T> onNext;
    private final BiConsumer<Span, Boolean> onComplete;
    private final BiConsumer<ContextView, SpanBuilder> builderConsumer;

    TraceFlux(Flux<? extends T> source,
              String name,
              Tracer tracer,
              BiConsumer<Span, T> onNext,
              BiConsumer<Span, Boolean> onComplete,
              BiConsumer<ContextView, SpanBuilder> builderConsumer) {
        super(source);
        this.spanName = name;
        this.tracer = tracer;
        this.onNext = onNext;
        this.builderConsumer = builderConsumer;
        this.onComplete = onComplete;
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
                this.builderConsumer.accept(context, builder);
            }

            Span span = builder
                    .setParent(ctx)
                    .startSpan();

            TraceMonoSubscriber<T> subscriber = new TraceMonoSubscriber<>(actual, span, onNext, onComplete);

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


    static class TraceMonoSubscriber<T> extends BaseSubscriber<T> implements Span {
        private final CoreSubscriber<? super T> actual;
        private final Span span;
        private final BiConsumer<Span, T> onNext;
        private final BiConsumer<Span, Boolean> onComplete;

        private volatile boolean hasValue;
        private volatile boolean stateSet;

        public TraceMonoSubscriber(CoreSubscriber<? super T> actual,
                                   Span span,
                                   BiConsumer<Span, T> onNext,
                                   BiConsumer<Span, Boolean> onComplete) {
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
            hasValue = true;
            actual.onNext(value);
        }

        @Override
        protected void hookOnComplete() {
            if (onComplete != null) {
                onComplete.accept(this, hasValue);
            }
            if (!stateSet) {
                span.setStatus(StatusCode.OK);
            }
            actual.onComplete();
        }

        @Override
        public <T> Span setAttribute(AttributeKey<T> key, T value) {
            span.setAttribute(key, value);
            return this;
        }

        @Override
        public Span addEvent(String name, Attributes attributes) {
            span.addEvent(name, attributes);
            return this;
        }

        @Override
        public Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
            span.addEvent(name, attributes, timestamp, unit);
            return this;
        }

        @Override
        public Span setStatus(StatusCode statusCode, String description) {
            stateSet = true;
            span.setStatus(statusCode, description);
            return this;
        }

        @Override
        public Span recordException(Throwable exception, Attributes additionalAttributes) {
            span.recordException(exception, additionalAttributes);
            return this;
        }

        @Override
        public Span updateName(String name) {
            span.updateName(name);
            return this;
        }

        @Override
        public void end() {

        }

        @Override
        public void end(long timestamp, TimeUnit unit) {

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
}