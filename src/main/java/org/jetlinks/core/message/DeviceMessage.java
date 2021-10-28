package org.jetlinks.core.message;

import org.jetlinks.core.metadata.Jsonable;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMessage extends ThingMessage, Jsonable {

    String getDeviceId();

    long getTimestamp();

    default String getThingId() {
        return getDeviceId();
    }

    @Override
    default <T> DeviceMessage addHeader(HeaderKey<T> header, T value) {
        ThingMessage.super.addHeader(header, value);
        return this;
    }

    @Override
    DeviceMessage addHeader(String header, Object value);

    @Override
    default <T> DeviceMessage addHeaderIfAbsent(HeaderKey<T> header, T value) {
        ThingMessage.super.addHeaderIfAbsent(header, value);
        return this;
    }

    @Override
    DeviceMessage addHeaderIfAbsent(String header, Object value);
}
