package org.jetlinks.core.device;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.metadata.DeviceMetadata;

/**
 * 设备产品型号操作
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceProductOperation extends Configurable {

    DeviceMetadata getMetadata();

    void updateMetadata(String metadata);

    DeviceProductInfo getInfo();

    void update(DeviceProductInfo info);

    ProtocolSupport getProtocol();

}
