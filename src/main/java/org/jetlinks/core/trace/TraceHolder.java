package org.jetlinks.core.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.apache.commons.collections.MapUtils;
import org.jetlinks.core.topic.Topic;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.function.Consumer3;
import reactor.util.context.ContextView;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 基于<a href="https://github.com/open-telemetry/opentelemetry-java/">OpenTelemetry</a>链路追踪工具类，
 * 用于进行响应式链路追踪的常用.
 * 支持动态全局开启关闭追踪,支持开启禁用特定的spanName,spanName支持类似url的洗粒度控制.
 *
 * @author zhouhao
 * @see MonoTracer
 * @see FluxTracer
 * @since 1.2
 */
public class TraceHolder {

    static final ContextKey<CharSequence> SPAN_NAME = ContextKey.named("spanName");

    //全局应用名: -Dtrace.app.name
    private static String GLOBAL_APP_NAME = System.getProperty("trace.app.name", "default");
    //是否全局开启: -Dtrace.enabled=true
    private static boolean traceEnabled = Boolean.parseBoolean(System.getProperty("trace.enabled", "true"));

    //禁用的spanName
    private static final Topic<String> disabledSpanName = Topic.createRoot();
    //启用的spanName
    private static final Topic<String> enabledSpanName = Topic.createRoot();

    private static OpenTelemetry telemetry;

    public static void setup(OpenTelemetry telemetry) {
        TraceHolder.telemetry = telemetry;
    }

    /**
     * 动态设置全局应用名
     */
    public static void setupGlobalName(String name) {
        GLOBAL_APP_NAME = name;
    }

    /**
     * @return 当前应用名
     */
    public static String appName() {
        return GLOBAL_APP_NAME;
    }

    /**
     * @return 是否全局开启
     */
    public static boolean isEnabled() {
        return traceEnabled && telemetry != null;
    }

    /**
     * @return 是否全局禁用
     */
    public static boolean isDisabled() {
        return telemetry == null || !isEnabled();
    }

    /**
     * 判断指定的spanName是否开启. spanName支持以/分割的树结构,
     * 如: /device/id/operation
     *
     * @param name spanName
     * @return 是否开启
     */
    public static boolean isEnabled(String name) {
        return isEnabled((CharSequence)name);
    }

    /**
     * 判断指定的spanName是否开启. spanName支持以/分割的树结构,
     * 如: /device/id/operation
     *
     * @param name spanName
     * @return 是否开启
     */
    public static boolean isEnabled(CharSequence name) {
        //全局关闭
        if (!traceEnabled) {
            return false;
        }
        AtomicReference<Boolean> enabled = new AtomicReference<>();
        //获取启用的span
        enabledSpanName
            .findTopic(name,
                       enabled,
                       (e,topic )-> e.set(true),
                       (e) -> {
                       });
        if (enabled.get() == null) {
            //获取禁用的span
            disabledSpanName
                .findTopic(name,
                           enabled,
                           (e,topic )-> e.set(false),
                           (e) -> {
                           });
        }
        if (enabled.get() == null) {
            return true;
        }

        return enabled.get();
    }


    /**
     * 判断指定的spanName是否禁用,与{@link #isEnabled(String)}逻辑相反
     *
     * @param name spanName
     * @return 是否开启
     */
    public static boolean isDisabled(String name) {
        return telemetry == null || !isEnabled(name);
    }

    public static boolean isDisabled(CharSequence name) {
        return telemetry == null || !isEnabled(name);
    }

    /**
     * 全局开启跟踪
     */
    public static void enable() {
        traceEnabled = true;
    }

    /**
     * 全局关闭跟踪
     */
    public static void disable() {
        traceEnabled = false;
    }

    /**
     * 指定handle,开启追踪指定的spanName.
     * <p>
     * spanName支持通配符,比如:{@code   /device/&#42;/name}
     * <p>
     * 在禁用时,需要使用开启时指定的handler进行禁用,或者调用返回值:{@link  Disposable#dispose()}来禁用spanName
     *
     * @param spanName spanName
     * @param handler  handler
     * @return Disposable
     */
    public static Disposable enable(String spanName, String handler) {
        removeDisabled(spanName, handler);
        Topic<String> subTable = enabledSpanName.append(spanName);
        subTable.subscribe(handler);
        return () -> subTable.unsubscribe(handler);
    }

    public static void removeEnabled(String spanName, String handler) {
        enabledSpanName
            .getTopic(spanName)
            .ifPresent(topic -> topic.unsubscribe(handler));
    }

    public static void removeDisabled(String spanName, String handler) {
        disabledSpanName
            .getTopic(spanName)
            .ifPresent(topic -> topic.unsubscribe(handler));
    }


    /**
     * 指定handler,禁用指定的spanName. 禁用的优先级低于启用.
     * <p>
     * spanName支持通配符,比如:{@code   /device/&#42;/name}
     * <p>
     * 在禁用时,需要使用开启时指定的handler进行禁用,或者调用返回值:{@link  Disposable#dispose()}来禁用spanName
     *
     * @param spanName spanName
     * @param handler  handler
     */
    public static void disable(String spanName, String handler) {
        disabledSpanName
            .append(spanName)
            .subscribe(handler);
        removeEnabled(spanName, handler);
    }

    /**
     * @return 获取当前的OpenTelemetry
     */
    public static OpenTelemetry telemetry() {
        return telemetry == null ? OpenTelemetry.noop() : telemetry;
    }


    /**
     * 使用指定的reactor上下文作为父级上下文,从指定的Map中获取跟踪信息并转换新的reactor上下文信息.
     * 可将转换后上下文应用到reactor操作符中进行链路追踪,如:
     *
     * <pre>{@code
     *
     *    service
     *    .doSomeThing()
     *    //传递追踪上下文到doSomeThing操作中
     *    .contextWrite(readToContext(Context.empty(),headers))
     *
     * }</pre>
     *
     * @param parent  父级上下文
     * @param carrier 追踪信息载体
     * @return 包含跟踪信息的上下文
     * @see io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
     * @see Context
     */
    public static reactor.util.context.Context readToContext(ContextView parent,
                                                             Map<String, ?> carrier) {
        if (TraceHolder.isDisabled() || MapUtils.isEmpty(carrier)) {
            return reactor.util.context.Context.of(parent);
        }
        return readToContext(parent, carrier, MapTextMapGetter.instance());
    }

    /**
     * 使用指定的reactor上下文作为父级上下文,从指定的对象中获取跟踪信息并转换新的reactor上下文信息.
     * 可将转换后上下文应用到reactor操作符中进行链路追踪,如:
     *
     * <pre>{@code
     *
     *    service
     *    .doSomeThing()
     *    //传递追踪上下文到doSomeThing操作中
     *    .contextWrite(readToContext(Context.empty(),data,getter))
     *
     * }</pre>
     *
     * @param parent 父级上下文
     * @param source 源对象
     * @param getter 跟踪信息载体
     * @return 包含跟踪信息的上下文
     * @see io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
     * @see Context
     */
    public static <T> reactor.util.context.Context readToContext(ContextView parent,
                                                                 T source,
                                                                 TextMapGetter<T> getter) {
        ContextPropagators propagators = TraceHolder.telemetry().getPropagators();
        TextMapPropagator propagator = propagators.getTextMapPropagator();

        Context context = parent.getOrDefault(Context.class, Context.current());
        if (null != context) {
            context = propagator.extract(context, source, getter);
            return reactor.util.context.Context.of(parent)
                                               .put(Context.class, context);
        }
        return reactor.util.context.Context.of(parent);
    }

    /**
     * 写出上下文中的跟踪信息到指定的载体中
     * <pre>{@code
     *
     *    TraceHolder
     *    .writeContextTo(ctx,new HashMap<>(), Map::put)
     *
     * }</pre>
     *
     * @param ctx     上下文
     * @param carrier 跟踪信息载体
     * @param setter  跟踪信息setter
     * @param <T>     载体泛型
     * @return 原始载体
     */
    public static <T> T writeContextTo(ContextView ctx,
                                       T carrier,
                                       Consumer3<T, String, String> setter) {
        if (isDisabled()) {
            return carrier;
        }
        ContextPropagators propagators = TraceHolder.telemetry().getPropagators();
        TextMapPropagator propagator = propagators.getTextMapPropagator();
        Context context = ctx.getOrDefault(Context.class, Context.current());
        if (null != context) {
            propagator.inject(context, carrier, setter::accept);
        }
        return carrier;
    }

    public static <T> T writeTo(T carrier,
                                Consumer3<T, String, String> setter) {
        if (isDisabled()) {
            return carrier;
        }
        ContextPropagators propagators = TraceHolder.telemetry().getPropagators();
        TextMapPropagator propagator = propagators.getTextMapPropagator();
        Context context = Context.current();
        if (null != context) {
            propagator.inject(context, carrier, setter::accept);
        }
        return carrier;
    }

    /**
     * 写出响应式操作符中的上下文到指定的载体中,注意,所有响应式操作符必须组合在一起.否则将无法获取到上下文信息.
     *
     * <pre>{@code
     * public class TraceExchangeFilterFunction implements ExchangeFilterFunction {
     *
     *     @Override
     *     @Nonnull
     *     public Mono<ClientResponse> filter(@Nonnull ClientRequest request,
     *                                        @Nonnull ExchangeFunction next) {
     *      return TraceHolder
     *            .writeContextTo(ClientRequest.from(request),
     *                            ClientRequest.Builder::header)
     *            .flatMap(builder -> next.exchange(builder.build()));
     *     }
     *
     * }
     * }</pre>
     *
     * @param carrier 跟踪信息载体
     * @param setter  跟踪信息setter
     * @param <T>     载体泛型
     * @return Mono
     */
    public static <T> Mono<T> writeContextTo(T carrier, Consumer3<T, String, String> setter) {
        if (isDisabled()) {
            return Mono.just(carrier);
        }
        return Mono
            .deferContextual(ctx -> Mono.just(writeContextTo(ctx, carrier, setter)));
    }

    /**
     * 复制源Map中的跟踪信息到指定的载体.
     * <pre>{@code
     *
     * TraceHolder
     *   .copyContext(request.headers(), message, Message::addHeader)
     *
     * }</pre>
     *
     * @param source  源
     * @param carrier 载体
     * @param setter  跟踪信息setter
     * @param <D>     载体泛型
     * @return 原始载体
     */
    public static <D> D copyContext(Map<String, ?> source,
                                    D carrier,
                                    Consumer3<D, String, String> setter) {
        if (isDisabled()) {
            return carrier;
        }
        return TraceHolder.writeContextTo(
            TraceHolder.readToContext(reactor.util.context.Context.empty(), source),
            carrier,
            setter);
    }

    public static void traceBlocking(String operation, Consumer<Span> task) {
        traceBlocking(operation, span -> {
            task.accept(span);
            return null;
        });
    }

    public static <R> R traceBlocking(String operation, Function<Span, R> function) {
        return traceBlocking(Context.current(), operation, function);
    }

    public static <R> R traceBlocking(Context context, String operation, Function<Span, R> function) {
        if (isDisabled(operation)) {
            return function.apply(Span.getInvalid());
        }
        Span span = telemetry
            .getTracer(appName())
            .spanBuilder(operation)
            .setParent(context)
            .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            return function.apply(span);
        } catch (Throwable e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

}
