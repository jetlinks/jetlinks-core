package org.jetlinks.core.message;

import org.jetlinks.core.metadata.Jsonable;

import java.io.Serializable;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMessage extends Jsonable, Serializable {

    String getMessageId();

    String getDeviceId();

    long getTimestamp();

}
