package org.jetlinks.core;

import org.apache.commons.collections.MapUtils;
import org.jetlinks.core.message.HeaderKey;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.utils.ConverterUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public interface HeaderSupport<SELF extends HeaderSupport<SELF>> extends Serializable {
    /**
     * 消息头,用于自定义一些消息行为, 默认的一些消息头请看:{@link Headers}
     *
     * @return headers or null
     * @see Headers
     */
    @Nullable
    Map<String, Object> getHeaders();

    /**
     * 添加一个header
     *
     * @param header header
     * @param value  value
     * @return this
     * @see Headers
     */
    SELF addHeader(String header, Object value);

    /**
     * 添加header,如果header已存在则放弃
     *
     * @param header header key
     * @param value  header 值
     * @return this
     */
    SELF addHeaderIfAbsent(String header, Object value);


    /**
     * 删除一个header
     *
     * @param header header
     * @return this
     * @see Headers
     */
    SELF removeHeader(String header);

    /**
     * @see Headers
     * @see Message#addHeader(String, Object)
     */
    default <T> SELF addHeader(HeaderKey<T> header, T value) {
        return addHeader(header.getKey(), value);
    }

    /**
     * @see Headers
     * @see Message#addHeaderIfAbsent(String, Object)
     */
    default <T> SELF addHeaderIfAbsent(HeaderKey<T> header, T value) {
        return addHeaderIfAbsent(header.getKey(), value);
    }

    default <T> T getOrAddHeader(HeaderKey<T> header, Supplier<T> value) {
        return this.computeHeader(header, (ignore, old) -> {
            if (old == null) {
                old = value.get();
            }
            return old;
        });
    }

    default <T> T getOrAddHeaderDefault(HeaderKey<T> header) {
        return getOrAddHeader(header, header::getDefaultValue);
    }

    @SuppressWarnings("all")
    default <T> Optional<T> getHeader(HeaderKey<T> key) {
        return Optional.ofNullable(getHeaderOrElse(key, null));
    }

    default <T> T getHeaderOrDefault(HeaderKey<T> key) {
        return getHeaderOrElse(key, key::getDefaultValue);
    }

    @SuppressWarnings("all")
    default <T> T getHeaderOrElse(HeaderKey<T> header, @Nullable Supplier<T> orElse) {
        Object val = getHeaderOrElse(header.getKey(), null);
        if (null == val) {
            return orElse == null ? null : orElse.get();
        }
        return ConverterUtils.convert(val, header);
    }

    default Object getHeaderOrElse(String header, @Nullable Supplier<Object> orElse) {
        Map<String, Object> headers = getHeaders();
        if (MapUtils.isEmpty(headers) || header == null) {
            return orElse == null ? null : orElse.get();
        }
        Object val = headers.get(header);
        if (val != null) {
            return val;
        }
        return orElse == null ? null : orElse.get();
    }

    default Optional<Object> getHeader(String header) {
        return Optional.ofNullable(getHeaderOrElse(header, null));
    }

    Object computeHeader(String key, BiFunction<String, Object, Object> computer);

    @SuppressWarnings("all")
    default <T> T computeHeader(HeaderKey<T> key, BiFunction<String, T, T> computer) {
        return (T) computeHeader(key.getKey(),
                                 (str, old) -> computer.apply(str, old == null ? null : ConverterUtils.convert(old, key)));
    }

    @SuppressWarnings("all")
    default SELF castSelf(){
        return (SELF) this;
    }

}
