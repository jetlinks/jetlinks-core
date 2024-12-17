package org.jetlinks.core.trace;

import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.commons.collections.MapUtils;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 用于跟踪reactor{@link Flux}操作符
 *
 * <pre>{@code
 *    //使用span.name作为跟踪名对source进行跟踪
 *    source
 *    .as(FluxTracer.create("span.name"));
 *
 *     //onNext,自定义跟踪属性
 *     source
 *      .as(FluxTracer
 *          .create("span.name",
 *                  (span,value)->{
 *                       span.setAttribute(key,value)
 *                  }
 *            ))
 *
 * }</pre>
 *
 * @param <T>
 * @see ReactiveTracerBuilder
 * @see TraceFlux
 * @since 1.2
 */
public interface FluxTracer<T> extends Function<Flux<T>, Flux<T>> {

    static <T> FluxTracer<T> unsupported() {
        return source -> source;
    }

    /**
     * 创建跟踪器构造器
     *
     * @param <T> 流元素泛型
     * @return 构造器
     */
    static <T> ReactiveTracerBuilder<FluxTracer<T>, T> builder() {
        return new FluxTracerBuilder<>();
    }

    /**
     * 使用默认的应用名作为作用域,使用指定的跟踪名创建跟踪器.
     * <pre>{@code
     *     return doSomeThing()
     *              .as(FluxTracer.create("span.name"))
     * }</pre>
     *
     * @param spanName spanName
     * @param <T>      流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(String spanName) {
        return create(TraceHolder.appName(), spanName);
    }

    /**
     * 使用默认的应用名作为作用域,使用指定的跟踪名创建跟踪器.
     * <pre>{@code
     *     return doSomeThing()
     *              .as(FluxTracer.create("span.name"))
     * }</pre>
     *
     * @param spanName spanName
     * @param <T>      流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(CharSequence spanName) {
        return create(TraceHolder.appName(), spanName);
    }

    /**
     * 使用指定的作用域以及跟踪名创建跟踪器.
     * <pre>{@code
     *     return doSomeThing()
     *              .as(FluxTracer.create("user.api","/user/created"))
     * }</pre>
     *
     * @param spanName spanName
     * @param <T>      流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(String scopeName,
                                    String spanName) {
        return create(scopeName, spanName, null, null);
    }

    /**
     * 使用指定的作用域以及跟踪名创建跟踪器.
     * <pre>{@code
     *     return doSomeThing()
     *              .as(FluxTracer.create("user.api","/user/created"))
     * }</pre>
     *
     * @param spanName spanName
     * @param <T>      流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(String scopeName,
                                    CharSequence spanName) {
        return create(scopeName, spanName, null, null);
    }

    /**
     * 使用指定的span名称以及跟踪名创建跟踪器.
     * <pre>{@code
     *     return doSomeThing()
     *              .as(FluxTracer.create(
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId())
     *                  ))
     * }</pre>
     *
     * @param spanName spanName
     * @param onNext   onNext回调
     * @param <T>      流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(String spanName,
                                    BiConsumer<ReactiveSpan, T> onNext) {
        return create(TraceHolder.appName(), spanName, onNext, null);
    }

    /**
     * 使用指定的span名称以及跟踪名创建跟踪器.
     * <pre>{@code
     *     return doSomeThing()
     *              .as(FluxTracer.create(
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId())
     *                  ))
     * }</pre>
     *
     * @param spanName spanName
     * @param onNext   onNext回调
     * @param <T>      流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(CharSequence spanName,
                                    BiConsumer<ReactiveSpan, T> onNext) {
        return create(TraceHolder.appName(), spanName, onNext, null);
    }


    /**
     * 使用指定的作用域和span名称以及跟踪名创建跟踪器.
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(FluxTracer.create(
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId())
     *                  ))
     * }</pre>
     *
     * @param spanName spanName
     * @param onNext   onNext回调
     * @param <T>      流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(String scopeName,
                                    String spanName,
                                    BiConsumer<ReactiveSpan, T> onNext) {
        return create(scopeName, spanName, onNext, null);
    }

    /**
     * 使用指定span名称以及自定义SpanBuilder创建跟踪器
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(FluxTracer.create(
     *                  "/user/created",
     *                  (builder)->builder.setAttribute("uid",id)
     *                  ))
     * }</pre>
     *
     * @param spanName        spanName
     * @param builderConsumer SpanBuilder 回调
     * @param <T>             流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(String spanName,
                                    Consumer<ReactiveSpanBuilder> builderConsumer) {
        return create(TraceHolder.appName(), spanName, null, builderConsumer);
    }

    /**
     * 使用指定的作用域和span名称以及自定义SpanBuilder创建跟踪器
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(FluxTracer.create(
     *                  "user.api",
     *                  "/user/created",
     *                  (builder)->builder.setAttribute("uid",id)
     *                  ))
     * }</pre>
     *
     * @param spanName        spanName
     * @param builderConsumer SpanBuilder 回调
     * @param <T>             流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(String scopeName,
                                    String spanName,
                                    Consumer<ReactiveSpanBuilder> builderConsumer) {
        return create(scopeName, spanName, null, builderConsumer);
    }


    /**
     * 使用指定的span名称,可通过onNext和builderConsumer来自定义span信息
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(FluxTracer.create(
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId()),
     *                  (builder)->builder.setAttribute("uid",id)
     *                  ))
     * }</pre>
     *
     * @param spanName        spanName
     * @param onNext          onNext回调
     * @param builderConsumer SpanBuilder 回调
     * @param <T>             流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(String spanName,
                                    BiConsumer<ReactiveSpan, T/*流中的数据*/> onNext,
                                    Consumer<ReactiveSpanBuilder> builderConsumer) {
        return create(TraceHolder.appName(), spanName, onNext, builderConsumer);
    }

    /**
     * 使用指定的span名称,可通过onNext和builderConsumer来自定义span信息
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(FluxTracer.create(
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId()),
     *                  (builder)->builder.setAttribute("uid",id)
     *                  ))
     * }</pre>
     *
     * @param spanName        spanName
     * @param onNext          onNext回调
     * @param builderConsumer SpanBuilder 回调
     * @param <T>             流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(CharSequence spanName,
                                    BiConsumer<ReactiveSpan, T/*流中的数据*/> onNext,
                                    Consumer<ReactiveSpanBuilder> builderConsumer) {
        return create(TraceHolder.appName(), spanName, onNext, builderConsumer);
    }

    /**
     * 使用指定的span名称,可通过onNext和onComplete来自定义span信息
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(FluxTracer.create(
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId()),
     *                  (span,total)->span.setAttribute("hasValue",total>0)
     *                  ))
     * }</pre>
     *
     * @param spanName   spanName
     * @param onNext     onNext回调
     * @param onComplete onComplete 回调
     * @param <T>        流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(String spanName,
                                    BiConsumer<ReactiveSpan, T> onNext,
                                    BiConsumer<ReactiveSpan, Long/*流中数据的数量*/> onComplete) {
        return create(TraceHolder.appName(), spanName, onNext, onComplete, null);
    }

    /**
     * 使用指定作用域和span名称,可通过onNext和builderConsumer来自定义span信息
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(FluxTracer.create(
     *                  "user.api",
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId()),
     *                  (builder)->builder.setAttribute("uid",id)
     *                  ))
     * }</pre>
     *
     * @param spanName        spanName
     * @param onNext          onNext回调
     * @param builderConsumer builderConsumer 回调
     * @param <T>             流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(String scopeName,
                                    String spanName,
                                    BiConsumer<ReactiveSpan, T/*流中的数据*/> onNext,
                                    Consumer<ReactiveSpanBuilder> builderConsumer) {
        return create(scopeName, spanName, onNext, null, builderConsumer);
    }

    /**
     * 使用指定作用域和span名称,可通过onNext和builderConsumer来自定义span信息
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(FluxTracer.create(
     *                  "user.api",
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId()),
     *                  (builder)->builder.setAttribute("uid",id)
     *                  ))
     * }</pre>
     *
     * @param spanName        spanName
     * @param onNext          onNext回调
     * @param builderConsumer builderConsumer 回调
     * @param <T>             流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(String scopeName,
                                    CharSequence spanName,
                                    BiConsumer<ReactiveSpan, T/*流中的数据*/> onNext,
                                    Consumer<ReactiveSpanBuilder> builderConsumer) {
        return create(scopeName, spanName, onNext, null, builderConsumer);
    }


    /**
     * 使用指定作用域和span名称,可通过onNext,onComplete和builderConsumer来自定义span信息
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(FluxTracer.create(
     *                  "user.api",
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId()),
     *                  (span,hasValue)->span.setAttribute("hasValue",hasValue),
     *                  (builder)->builder.setAttribute("uid",id)
     *                  ))
     * }</pre>
     *
     * @param spanName        spanName
     * @param onNext          onNext回调
     * @param builderConsumer builderConsumer 回调
     * @param <T>             流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(String scopeName,
                                    String spanName,
                                    BiConsumer<ReactiveSpan, T> onNext,
                                    BiConsumer<ReactiveSpan, Long> onComplete,
                                    Consumer<ReactiveSpanBuilder> builderConsumer) {
        if (TraceHolder.isDisabled(spanName)) {
            return unsupported();
        }
        return FluxTracerBuilder
                .<T>create(true)
                .scopeName(scopeName)
                .spanName(spanName)
                .onNext(onNext)
                .onComplete(onComplete)
                .onSubscription(builderConsumer)
                .build();
    }

    /**
     * 使用指定作用域和span名称,可通过onNext,onComplete和builderConsumer来自定义span信息
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(FluxTracer.create(
     *                  "user.api",
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId()),
     *                  (span,hasValue)->span.setAttribute("hasValue",hasValue),
     *                  (builder)->builder.setAttribute("uid",id)
     *                  ))
     * }</pre>
     *
     * @param spanName        spanName
     * @param onNext          onNext回调
     * @param builderConsumer builderConsumer 回调
     * @param <T>             流元素类型
     * @return FluxTracer
     */
    static <T> FluxTracer<T> create(String scopeName,
                                    CharSequence spanName,
                                    BiConsumer<ReactiveSpan, T> onNext,
                                    BiConsumer<ReactiveSpan, Long> onComplete,
                                    Consumer<ReactiveSpanBuilder> builderConsumer) {
        if (TraceHolder.isDisabled(spanName)) {
            return unsupported();
        }
        return FluxTracerBuilder
            .<T>create(true)
            .scopeName(scopeName)
            .spanName(spanName)
            .onNext(onNext)
            .onComplete(onComplete)
            .onSubscription(builderConsumer)
            .build();
    }

    /**
     * 使用指定的跟踪信息载体来创建跟踪器
     * <pre>
     *     source.as(FluxTracer.createWith(httpHeaders));
     * </pre>
     *
     * @param carrier
     * @param <R>
     * @return
     */
    @SuppressWarnings("all")
    static <R> FluxTracer<R> createWith(Map<String, ?> carrier) {
        if (TraceHolder.isDisabled() || MapUtils.isEmpty(carrier)) {
            return unsupported();
        }
        return createWith(carrier, MapTextMapGetter.instance());
    }

    /**
     * 基于指定的跟踪信息载体来构造追踪器，如: 通过http header来获取上下文并进行追踪
     * <pre>{@code
     *
     *     chain
     *     .filter(exchange)
     *     .as(createWith(headers,HttpHeaders::getFirst))
     * }
     * </pre>
     *
     * @param carrier 信号
     * @param <R>
     * @return 追踪器
     */
    @SuppressWarnings("all")
    static <T, R> FluxTracer<R> createWith(T source, TextMapGetter<T> getter) {
        if (TraceHolder.isDisabled()) {
            return unsupported();
        }
        return flux -> flux
                .contextWrite(ctx -> TraceHolder.readToContext(ctx, source, getter));
    }


}
