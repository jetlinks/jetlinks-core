package org.jetlinks.core.trace;

import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.commons.collections.MapUtils;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 用于跟踪reactor{@link Mono}操作符
 *
 * <pre>{@code
 *    //使用span.name作为跟踪名对source进行跟踪
 *    source
 *    .as(MonoTracer.create("span.name"));
 *
 *     //onNext,自定义跟踪属性
 *     source
 *      .as(MonoTracer
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
 * @see TraceMono
 * @since 1.2
 */
public interface MonoTracer<T> extends Function<Mono<T>, Mono<T>> {

    //直接返回原始Mono
    static <T> MonoTracer<T> unsupported() {
        return source -> source;
    }

    /**
     * 创建跟踪器构造器
     *
     * @param <T> 流元素泛型
     * @return 构造器
     */
    static <T> ReactiveTracerBuilder<MonoTracer<T>, T> builder() {
        return MonoTracerBuilder.create(true);
    }

    /**
     * 使用默认的应用名作为作用域,使用指定的跟踪名创建跟踪器.
     * <pre>{@code
     *     return doSomeThing()
     *              .as(MonoTracer.create("span.name"))
     * }</pre>
     *
     * @param spanName spanName
     * @param <T>      流元素类型
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(String spanName) {
        return create(TraceHolder.appName(), spanName);
    }

    /**
     * 使用默认的应用名作为作用域,使用指定的跟踪名创建跟踪器.
     * <pre>{@code
     *     return doSomeThing()
     *              .as(MonoTracer.create("span.name"))
     * }</pre>
     *
     * @param spanName spanName
     * @param <T>      流元素类型
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(CharSequence spanName) {
        return create(TraceHolder.appName(), spanName);
    }

    /**
     * 使用指定的作用域以及跟踪名创建跟踪器.
     * <pre>{@code
     *     return doSomeThing()
     *              .as(MonoTracer.create("user.api","/user/created"))
     * }</pre>
     *
     * @param spanName spanName
     * @param <T>      流元素类型
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(String scopeName,
                                    String spanName) {
        return create(scopeName, spanName, null, null);
    }

    /**
     * 使用指定的作用域以及跟踪名创建跟踪器.
     * <pre>{@code
     *     return doSomeThing()
     *              .as(MonoTracer.create("user.api","/user/created"))
     * }</pre>
     *
     * @param spanName spanName
     * @param <T>      流元素类型
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(String scopeName,
                                    CharSequence spanName) {
        return create(scopeName, spanName, null, null);
    }

    /**
     * 使用指定的span名称以及跟踪名创建跟踪器.
     * <pre>{@code
     *     return doSomeThing()
     *              .as(MonoTracer.create(
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId())
     *                  ))
     * }</pre>
     *
     * @param spanName spanName
     * @param onNext   onNext回调
     * @param <T>      流元素类型
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(String spanName,
                                    BiConsumer<ReactiveSpan, T> onNext) {
        return create(TraceHolder.appName(), spanName, onNext, null);
    }

    /**
     * 使用指定的span名称以及跟踪名创建跟踪器.
     * <pre>{@code
     *     return doSomeThing()
     *              .as(MonoTracer.create(
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId())
     *                  ))
     * }</pre>
     *
     * @param spanName spanName
     * @param onNext   onNext回调
     * @param <T>      流元素类型
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(CharSequence spanName,
                                    BiConsumer<ReactiveSpan, T> onNext) {
        return create(TraceHolder.appName(), spanName, onNext, null);
    }

    /**
     * 使用指定的作用域和span名称以及跟踪名创建跟踪器.
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(MonoTracer.create(
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId())
     *                  ))
     * }</pre>
     *
     * @param spanName spanName
     * @param onNext   onNext回调
     * @param <T>      流元素类型
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(String scopeName,
                                    String spanName,
                                    BiConsumer<ReactiveSpan, T> onNext) {
        return create(scopeName, spanName, onNext, null);
    }

    /**
     * 使用指定span名称以及自定义SpanBuilder创建跟踪器
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(MonoTracer.create(
     *                  "/user/created",
     *                  (builder)->builder.setAttribute("uid",id)
     *                  ))
     * }</pre>
     *
     * @param spanName        spanName
     * @param builderConsumer SpanBuilder 回调
     * @param <T>             流元素类型
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(String spanName,
                                    Consumer<ReactiveSpanBuilder> builderConsumer) {
        return create(TraceHolder.appName(), spanName, null, builderConsumer);
    }

    /**
     * 使用指定span名称以及自定义SpanBuilder创建跟踪器
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(MonoTracer.create(
     *                  "/user/created",
     *                  (builder)->builder.setAttribute("uid",id)
     *                  ))
     * }</pre>
     *
     * @param spanName        spanName
     * @param builderConsumer SpanBuilder 回调
     * @param <T>             流元素类型
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(CharSequence spanName,
                                    Consumer<ReactiveSpanBuilder> builderConsumer) {
        return create(TraceHolder.appName(), spanName, null, builderConsumer);
    }
    /**
     * 使用指定的作用域和span名称以及自定义SpanBuilder创建跟踪器
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(MonoTracer.create(
     *                  "user.api",
     *                  "/user/created",
     *                  (builder)->builder.setAttribute("uid",id)
     *                  ))
     * }</pre>
     *
     * @param spanName        spanName
     * @param builderConsumer SpanBuilder 回调
     * @param <T>             流元素类型
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(String scopeName,
                                    String spanName,
                                    Consumer<ReactiveSpanBuilder> builderConsumer) {
        return create(scopeName, spanName, null, builderConsumer);
    }


    /**
     * 使用指定的span名称,可通过onNext和builderConsumer来自定义span信息
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(MonoTracer.create(
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
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(String spanName,
                                    BiConsumer<ReactiveSpan, T/*流中的数据*/> onNext,
                                    Consumer<ReactiveSpanBuilder> builderConsumer) {
        return create(TraceHolder.appName(), spanName, onNext, builderConsumer);
    }

    /**
     * 使用指定的span名称,可通过onNext和onComplete来自定义span信息
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(MonoTracer.create(
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId()),
     *                  (span,hasValue)->span.setAttribute("hasValue",hasValue)
     *                  ))
     * }</pre>
     *
     * @param spanName   spanName
     * @param onNext     onNext回调
     * @param onComplete onComplete 回调
     * @param <T>        流元素类型
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(String spanName,
                                    BiConsumer<ReactiveSpan, T> onNext,
                                    BiConsumer<ReactiveSpan, Boolean/*流中是否有值*/> onComplete) {
        return create(TraceHolder.appName(), spanName, onNext, onComplete, null);
    }

    /**
     * 使用指定的span名称,可通过onNext和onComplete来自定义span信息
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(MonoTracer.create(
     *                  "/user/created",
     *                  (span,user)->span.setAttribute(userId,user.getId()),
     *                  (span,hasValue)->span.setAttribute("hasValue",hasValue)
     *                  ))
     * }</pre>
     *
     * @param spanName   spanName
     * @param onNext     onNext回调
     * @param onComplete onComplete 回调
     * @param <T>        流元素类型
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(CharSequence spanName,
                                    BiConsumer<ReactiveSpan, T> onNext,
                                    BiConsumer<ReactiveSpan, Boolean/*流中是否有值*/> onComplete) {
        return create(TraceHolder.appName(), spanName, onNext, onComplete, null);
    }

    /**
     * 使用指定作用域和span名称,可通过onNext和builderConsumer来自定义span信息
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(MonoTracer.create(
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
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(String scopeName,
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
     *     .as(MonoTracer.create(
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
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(String scopeName,
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
     *     .as(MonoTracer.create(
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
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(String scopeName,
                                    String spanName,
                                    BiConsumer<ReactiveSpan, T> onNext,
                                    BiConsumer<ReactiveSpan, Boolean> onComplete,
                                    Consumer<ReactiveSpanBuilder> builderConsumer) {
        //全局关闭了,这里不判断spanName是否禁用,因为在执行时还会判断.
        if (TraceHolder.isDisabled()) {
            return unsupported();
        }
        return MonoTracerBuilder
                .<T>create(true)
                .scopeName(scopeName)
                .spanName(spanName)
                .onNext(onNext)
                .onComplete(onComplete != null ? (span, total) -> onComplete.accept(span, total > 0) : null)
                .onSubscription(builderConsumer)
                .build();
    }

    /**
     * 使用指定作用域和span名称,可通过onNext,onComplete和builderConsumer来自定义span信息
     * <pre>{@code
     *     return this
     *     .doSomeThing()
     *     .as(MonoTracer.create(
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
     * @return MonoTracer
     */
    static <T> MonoTracer<T> create(String scopeName,
                                    CharSequence spanName,
                                    BiConsumer<ReactiveSpan, T> onNext,
                                    BiConsumer<ReactiveSpan, Boolean> onComplete,
                                    Consumer<ReactiveSpanBuilder> builderConsumer) {
        //全局关闭了,这里不判断spanName是否禁用,因为在执行时还会判断.
        if (TraceHolder.isDisabled()) {
            return unsupported();
        }
        return MonoTracerBuilder
            .<T>create(true)
            .scopeName(scopeName)
            .spanName(spanName)
            .onNext(onNext)
            .onComplete(onComplete != null ? (span, total) -> onComplete.accept(span, total > 0) : null)
            .onSubscription(builderConsumer)
            .build();
    }

    /**
     * 使用指定的跟踪信息载体来创建跟踪器
     * <pre>
     *     source.as(MonoTracer.createWith(httpHeaders));
     * </pre>
     *
     * @param carrier
     * @param <R>
     * @return
     */
    @SuppressWarnings("all")
    static <R> MonoTracer<R> createWith(Map<String, ?> carrier) {
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
    static <T, R> MonoTracer<R> createWith(T source, TextMapGetter<T> getter) {
        if (TraceHolder.isDisabled()) {
            return unsupported();
        }
        return mono -> mono
                .contextWrite(ctx -> TraceHolder.readToContext(ctx, source, getter));
    }


}
