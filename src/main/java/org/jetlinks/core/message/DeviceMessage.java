package org.jetlinks.core.message;

import org.jetlinks.core.metadata.Jsonable;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMessage extends Message, Jsonable {

    String getDeviceId();

    long getTimestamp();

}
