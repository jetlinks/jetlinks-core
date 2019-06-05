package org.jetlinks.core.message;

import org.jetlinks.core.metadata.Jsonable;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMessage extends Jsonable, Serializable {

    String getMessageId();

    String getDeviceId();

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

    default Optional<Object> getHeader(String header) {
        return Optional.ofNullable(getHeaders())
                .map(headers -> headers.get(header));
    }
}
