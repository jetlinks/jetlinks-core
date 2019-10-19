package org.jetlinks.core.message;

import java.util.Map;
import java.util.Optional;

public interface Message {

    String getMessageId();

    long getTimestamp();

    /**
     * @return headers or null
     * @see Headers
     */
    Map<String, Object> getHeaders();

    /**
     * 添加一个header
     *
     * @param header header
     * @param value  value
     * @return this
     * @see Headers
     */
    DeviceMessage addHeader(String header, Object value);

    /**
     * 删除一个header
     *
     * @param header header
     * @return this
     * @see Headers
     */
    DeviceMessage removeHeader(String header);

    default <T> DeviceMessage addHeader(HeaderKey<T> header, Object value) {
        return addHeader(header.getKey(), value);
    }

    default <T> Optional<T> getHeader(HeaderKey<T> key) {
        return getHeader(key.getKey())
                .map(v -> (T) v);
    }

    default Optional<Object> getHeader(String header) {
        return Optional.ofNullable(getHeaders())
                .map(headers -> headers.get(header));
    }
}
