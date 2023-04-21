package org.jetlinks.core.monitor.tracer;

import io.opentelemetry.api.trace.SpanBuilder;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.trace.ReactiveTracerBuilder;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 链路追踪器
 *
 * @author zhouhao
 * @since 1.2.1
 */
public interface Tracer {

    /**
     * 创建一个延迟加载的链路追踪器,用于解决链路追踪器延后加载时无法获取等场景.
     *
     * @param lazy 链路追踪器提供商
     * @return Tracer
     */
    static Tracer lazy(Supplier<Tracer> lazy) {
        return new ProxyTracer(lazy);
    }

    /**
     * 对Flux进行追踪
     * <pre>{@code
     *  tracer().traceFlux("request",(builder)->{
     *
     *      builder
     *      .onSubscription((ctx,span)->{
     *
     *          span.setAttribute(REQUEST,requestInfo.toString());
     *
     *      })
     *      .onNext((span,response)->{
     *
     *         span.setAttribute(RESPONSE,response.toString());
     *
     *      })
     *
     *  })
     * }</pre>
     *
     * @param operation 操作
     * @param consumer  追踪构造器
     * @param <E>       元素类型
     * @return FluxTracer
     */
    <E> FluxTracer<E> traceFlux(String operation,
                                Consumer<ReactiveTracerBuilder<FluxTracer<E>, E>> consumer);

    /**
     * 对Flux进行追踪
     * <pre>{@code
     *  mono.as(tracer()
     *    .traceFlux("request",(ctx,span)->{
     *
     *          span.setAttribute(REQUEST,requestInfo.toString());
     *
     *  }))
     * }</pre>
     *
     * @param operation 操作
     * @param consumer  追踪构造器
     * @param <E>       元素类型
     * @return FluxTracer
     */
    default <E> FluxTracer<E> traceFlux(String operation,
                                        BiConsumer<ContextView, SpanBuilder> consumer) {
        return traceFlux(operation, (builder) -> builder.onSubscription(consumer));
    }

    /**
     * 对Mono进行追踪
     * <pre>{@code
     *
     *  mono.as(tracer()
     *      .traceMono("request",(builder)->{
     *           builder
     *           .onSubscription((ctx,span)->{
     *               span.setAttribute(REQUEST,requestInfo.toString());
     *            })
     *           .onNext((span,response)->{
     *               span.setAttribute(RESPONSE,response.toString());
     *           })
     *       }))
     *
     * }</pre>
     *
     * @param operation 操作
     * @param consumer  追踪构造器
     * @param <E>       元素类型
     * @return FluxTracer
     */

    <E> MonoTracer<E> traceMono(String operation,
                                Consumer<ReactiveTracerBuilder<MonoTracer<E>, E>> consumer);

    /**
     * 对Mono进行追踪
     * <pre>{@code
     *
     *  mono.as(tracer()
     *      .traceMono("request",(ctx,span)->{
     *
     *           span.setAttribute(REQUEST,requestInfo.toString());
     *
     *       }))
     *
     * }</pre>
     *
     * @param operation 操作
     * @param consumer  追踪构造器
     * @param <E>       元素类型
     * @return FluxTracer
     */
    default <E> MonoTracer<E> traceMono(String operation,
                                        BiConsumer<ContextView, SpanBuilder> consumer) {
        return traceMono(operation, (builder) -> builder.onSubscription(consumer));
    }
}
