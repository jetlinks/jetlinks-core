package org.jetlinks.core.message;

import org.jetlinks.core.metadata.Jsonable;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMessage extends Message, Jsonable {

    String getDeviceId();

    long getTimestamp();

    @Override
    default <T> DeviceMessage addHeader(HeaderKey<T> header, T value) {
        Message.super.addHeader(header, value);
        return this;
    }

    @Override
    DeviceMessage addHeader(String header, Object value);

    @Override
    default <T> DeviceMessage addHeaderIfAbsent(HeaderKey<T> header, T value) {
        Message.super.addHeaderIfAbsent(header, value);
        return this;
    }

    @Override
    DeviceMessage addHeaderIfAbsent(String header, Object value);
}
