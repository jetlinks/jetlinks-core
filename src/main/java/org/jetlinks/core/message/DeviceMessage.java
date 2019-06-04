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

    Map<String, Object> getHeaders();

    DeviceMessage addHeader(String header, Object value);

    default Optional<Object> getHeader(String header) {
        return Optional.ofNullable(getHeaders())
                .map(headers -> headers.get(header));
    }
}
