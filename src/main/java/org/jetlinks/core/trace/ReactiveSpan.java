package org.jetlinks.core.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 响应式的span构建器,主要拓展了{@link ReactiveSpan#setAttributeLazy(AttributeKey, Supplier)}以提升性能
 *
 * @author zhouhao
 * @see 1.2.1
 */
public interface ReactiveSpan extends Span {

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
    <T> ReactiveSpan setAttributeLazy(AttributeKey<T> key, Supplier<T> lazyValue);

    /**
     * 设置延迟获取的属性值,通常在属性值需要计算时(比如转为json)使用以提升性能.
     *
     * <pre>{@code
     *      setAttributeLazy(BODY,body,Body::toJsonString)
     * }</pre>
     *
     * @param key       Key
     * @param lazyValue 延迟加载值
     * @param <T>       值类型
     * @return ReactiveSpanBuilder
     * @since 1.2.3
     */
    default <V, T> ReactiveSpan setAttributeLazy(AttributeKey<T> key, V value, Function<V, T> lazyValue) {
        return setAttributeLazy(key, () -> lazyValue.apply(value));
    }

    /**
     * @see Span#setAttribute(AttributeKey, Object)
     */
    @Override
    <T> ReactiveSpan setAttribute(@Nonnull AttributeKey<T> key, @Nonnull T value);

    /**
     * @see Span#setAttribute(String, boolean)
     */
    @Override
    ReactiveSpan setStatus(@Nonnull StatusCode statusCode, @Nonnull String description);


    /**
     * @see Span#setAttribute(String, boolean)
     */
    @Override
    default ReactiveSpan setAttribute(@Nonnull String key, long value) {
        Span.super.setAttribute(key, value);
        return this;
    }

    /**
     * @see Span#setAttribute(String, boolean)
     */
    @Override
    default ReactiveSpan setAttribute(@Nonnull String key, double value) {
        Span.super.setAttribute(key, value);
        return this;
    }

    /**
     * @see Span#setAttribute(String, boolean)
     */
    @Override
    default ReactiveSpan setAttribute(@Nonnull String key, @Nonnull String value) {
        Span.super.setAttribute(key, value);
        return this;
    }

    /**
     * @see Span#setAttribute(String, boolean)
     */
    @Override
    default ReactiveSpan setAttribute(@Nonnull String key, boolean value) {
        Span.super.setAttribute(key, value);
        return this;
    }

    /**
     * @see Span#setAttribute(String, boolean)
     */
    @Override
    default ReactiveSpan setAttribute(@Nonnull AttributeKey<Long> key, int value) {
        Span.super.setAttribute(key, value);
        return this;
    }

    /**
     * @see Span#setAttribute(String, boolean)
     */
    @Override
    default ReactiveSpan setStatus(@Nonnull StatusCode statusCode) {
        Span.super.setStatus(statusCode);
        return this;
    }

}
