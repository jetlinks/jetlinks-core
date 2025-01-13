package org.jetlinks.core.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 响应式的span构建器,主要拓展了{@link ReactiveSpanBuilder#setAttributeLazy(AttributeKey, Supplier)}以提升性能
 *
 * @author zhouhao
 * @see SpanBuilder
 * @see MonoTracer
 * @see FluxTracer
 * @since 1.2.1
 */
public interface ReactiveSpanBuilder extends SpanBuilder {

    /**
     * 设置延迟获取的属性值,通常在属性值需要计算时(比如转为json)使用以提升性能.
     *
     * <pre>{@code
     *      setAttributeLazy(BODY,body::toJsonString)
     * }</pre>
     *
     * @param key       Key
     * @param lazyValue 延迟加载值
     * @param <T>       值类型
     * @return ReactiveSpanBuilder
     */
    <T> ReactiveSpanBuilder setAttributeLazy(AttributeKey<T> key, Supplier<T> lazyValue);

    /**
     * 设置延迟获取的属性值,通常在属性值需要计算时(比如转为json)使用以提升性能.
     *
     * <pre>{@code
     *      setAttributeLazy(BODY,body,Body::toJsonString)
     * }</pre>
     *
     * @param key       Key
     * @param value     值
     * @param converter 转换器
     * @param <T>       值类型
     * @return ReactiveSpanBuilder
     * @since 1.2.3
     */
    default <V, T> ReactiveSpanBuilder setAttributeLazy(AttributeKey<T> key, V value, Function<V, T> converter) {
        return setAttributeLazy(key, () -> converter.apply(value));
    }

    /**
     * @see SpanBuilder#setAttribute(AttributeKey, Object)
     */
    @Override
    <T> ReactiveSpanBuilder setAttribute(@Nonnull AttributeKey<T> key, @Nonnull T value);

    /**
     * @see SpanBuilder#setAttribute(String, boolean)
     */
    @Override
    ReactiveSpanBuilder setAttribute(@Nonnull String key, boolean value);

    /**
     * @see SpanBuilder#setAttribute(String, String)
     */
    @Override
    ReactiveSpanBuilder setAttribute(@Nonnull String key, @Nonnull String value);

    /**
     * @see SpanBuilder#setAttribute(String, double)
     */
    @Override
    ReactiveSpanBuilder setAttribute(@Nonnull String key, double value);

    /**
     * @see SpanBuilder#setAttribute(String, long)
     */
    @Override
    ReactiveSpanBuilder setAttribute(@Nonnull String key, long value);

    /**
     * @see SpanBuilder#setNoParent
     */
    @Override
    ReactiveSpanBuilder setNoParent();

    /**
     * @see SpanBuilder#setParent(Context)
     */
    @Override
    ReactiveSpanBuilder setParent(@Nonnull Context context);

    /**
     * @see SpanBuilder#setSpanKind(SpanKind)
     */
    @Override
    ReactiveSpanBuilder setSpanKind(@Nonnull SpanKind spanKind);

    /**
     * @see SpanBuilder#setStartTimestamp(long, TimeUnit)
     */
    @Override
    ReactiveSpanBuilder setStartTimestamp(long startTimestamp, @Nonnull TimeUnit unit);

    /**
     * @see SpanBuilder#setStartTimestamp(Instant)
     */
    @Override
    default ReactiveSpanBuilder setStartTimestamp(@Nonnull Instant startTimestamp) {
        SpanBuilder.super.setStartTimestamp(startTimestamp);
        return this;
    }

    /**
     * @see SpanBuilder#setAllAttributes(Attributes)
     */
    @Override
    default ReactiveSpanBuilder setAllAttributes(@Nonnull Attributes attributes) {
        SpanBuilder.super.setAllAttributes(attributes);
        return this;
    }
}
