package org.jetlinks.core.device;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceState {

    byte unknown = 0;

    byte online = 1;

    byte noActive = -3;

    byte offline = -1;

    byte timeout = -2;

}
