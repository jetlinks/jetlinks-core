package org.jetlinks.core.monitor.tracer;

import io.opentelemetry.api.trace.Span;
import lombok.SneakyThrows;
import org.jetlinks.core.trace.*;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.concurrent.Callable;
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
     * @return 什么也不做的Tracer
     */
    static Tracer noop() {
        return NoopTracer.INSTANCE;
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

    default <E> FluxTracer<E> traceFlux(CharSequence operation,
                                        Consumer<ReactiveTracerBuilder<FluxTracer<E>, E>> consumer) {
        return traceFlux(operation.toString(), consumer);
    }


    /**
     * 对Flux进行追踪
     * <pre>{@code
     *
     *  flux.as(tracer()
     *      .traceFlux("request"))
     *
     * }</pre>
     *
     * @param operation 操作标识
     * @param <E>       E
     * @return MonoTracer
     */
    default <E> FluxTracer<E> traceFlux(String operation) {
        return traceFlux(operation, ignore -> {
        });
    }

    default <E> FluxTracer<E> traceFlux(CharSequence operation) {
        return traceFlux(operation, ignore -> {
        });
    }

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
                                        BiConsumer<ContextView, ReactiveSpanBuilder> consumer) {
        return traceFlux(operation, (builder) -> builder.onSubscription(consumer));
    }

    default <E> FluxTracer<E> traceFlux(CharSequence operation,
                                        BiConsumer<ContextView, ReactiveSpanBuilder> consumer) {
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

    default <E> MonoTracer<E> traceMono(CharSequence operation,
                                        Consumer<ReactiveTracerBuilder<MonoTracer<E>, E>> consumer) {
        return traceMono(operation.toString(), consumer);
    }

    default <E> MonoTracer<E> traceMono(CharSequence operation) {
        return traceMono(operation, ignore -> {
        });
    }


    /**
     * 对Mono进行追踪
     * <pre>{@code
     *
     *  mono.as(tracer()
     *      .traceMono("request"))
     *
     * }</pre>
     *
     * @param operation 操作标识
     * @param <E>       E
     * @return MonoTracer
     */
    default <E> MonoTracer<E> traceMono(String operation) {
        return traceMono(operation, ignore -> {
        });
    }

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
                                        BiConsumer<ContextView, ReactiveSpanBuilder> consumer) {
        return traceMono(operation, (builder) -> builder.onSubscription(consumer));
    }

    default <E> E traceBlocking(CharSequence operation,
                                ContextView ctx,
                                Function<ReactiveSpan, E> task) {
        return task.apply(ReactiveSpan.wrap(Span.current()));
    }

    default <E> E traceBlocking(CharSequence operation, Function<ReactiveSpan, E> task) {
        return traceBlocking(operation, Context.empty(), task);
    }
}
