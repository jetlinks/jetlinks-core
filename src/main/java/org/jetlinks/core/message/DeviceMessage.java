package org.jetlinks.core.message;

import org.jetlinks.core.metadata.Jsonable;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMessage extends Message, Jsonable, Serializable {

    String getDeviceId();

    long getTimestamp();
}
