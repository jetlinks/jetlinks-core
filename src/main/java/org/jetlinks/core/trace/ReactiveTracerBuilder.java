package org.jetlinks.core.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import reactor.function.Consumer3;
import reactor.util.context.ContextView;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 响应式跟踪构造器
 *
 * @param <T> 跟踪器类型
 * @param <E> 响应式流元素类型
 * @see FluxTracer
 * @see MonoTracer
 */
public interface ReactiveTracerBuilder<T, E> {

    /**
     * 作用域,通常是库,包名或者类名
     *
     * @param name 作用域名
     * @return this
     * @see OpenTelemetry#getTracer(String)
     */
    ReactiveTracerBuilder<T, E> scopeName(@Nonnull String name);

    /**
     * 定义跟踪名称
     *
     * @param name 名称
     * @return this
     */
    ReactiveTracerBuilder<T, E> spanName(@Nonnull String name);

    /**
     * 定义跟踪名称
     *
     * @param name 名称
     * @return this
     */
    default ReactiveTracerBuilder<T, E> spanName(@Nonnull CharSequence name) {
        return spanName(ctx -> name);
    }

    /**
     * 定义跟踪名称
     *
     * @param nameBuilder 名称
     * @return this
     */
    ReactiveTracerBuilder<T, E> spanName(@Nonnull Function<ContextView, CharSequence> nameBuilder);

    /**
     * 监听流中的数据,并进行span自定义. 当流中产生数据时,回调函数被调用.
     *
     * @param callback 回调
     * @return this
     * @see Span
     * @see reactor.core.publisher.Flux#doOnNext(Consumer)
     */
    default ReactiveTracerBuilder<T, E> onNext(BiConsumer<ReactiveSpan, E> callback) {
        return callback == null ? this : onNext((ctx, span, val) -> callback.accept(span, val));
    }

    /**
     * 监听流中的数据,并进行span自定义. 当流中产生数据时,回调函数被调用.
     *
     * @param callback 回调
     * @return this
     * @see Span
     * @see reactor.core.publisher.Flux#doOnNext(Consumer)
     */
    ReactiveTracerBuilder<T, E> onNext(Consumer3<ContextView, ReactiveSpan, E> callback);

    /**
     * 监听流完成,流完成时,回调函数被调用
     *
     * @param callback 回调函数
     * @return this
     * @see reactor.core.publisher.Flux#doOnComplete(Runnable)
     */
    default ReactiveTracerBuilder<T, E> onComplete(BiConsumer<ReactiveSpan, Long> callback) {
        return callback == null ? this : onComplete((contextView, span, aLong) -> callback.accept(span, aLong));
    }

    /**
     * 监听流完成,流完成时,回调函数被调用
     *
     * @param callback 回调函数
     * @return this
     * @see reactor.core.publisher.Flux#doOnComplete(Runnable)
     */
    ReactiveTracerBuilder<T, E> onComplete(Consumer3<ContextView, ReactiveSpan, Long> callback);

    /**
     * 监听流错误,当流发生异常时,回调函数被调用
     *
     * @param callback 回调函数
     * @return this
     * @see reactor.core.publisher.Flux#doOnError(Consumer)
     */
    ReactiveTracerBuilder<T, E> onError(BiConsumer<ContextView, Throwable> callback);


    /**
     * 当流被订阅时,回调函数被调用,可以使用回调中的上下文以及span builder来进行自定义.
     *
     * @param callback 回调函数
     * @return this
     */
    ReactiveTracerBuilder<T, E> onSubscription(BiConsumer<ContextView, ReactiveSpanBuilder> callback);

    /**
     * 当流被订阅时,回调函数被调用,可以对span builder进行自定义.
     *
     * @param callback 回调函数
     * @return this
     */
    ReactiveTracerBuilder<T, E> onSubscription(Consumer<ReactiveSpanBuilder> callback);

    ReactiveTracerBuilder<T, E> defaultContext(Supplier<Context> defaultContext);

    /**
     * 构造跟踪器
     *
     * @return 跟踪器
     */
    T build();
}
