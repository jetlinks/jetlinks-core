package org.jetlinks.core.device;

import org.jetlinks.core.Configurable;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.metadata.DeviceMetadata;

/**
 * 设备产品型号操作
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceProductOperation extends Configurable {

    /**
     * @return 设备产品型号元数据信息
     */
    DeviceMetadata getMetadata();

    /**
     * 更新设备型号元数据信息
     *
     * @param metadata 元数据信息
     */
    void updateMetadata(String metadata);

    /**
     * @return 设备产品信息
     */
    DeviceProductInfo getInfo();

    /**
     * 更新产品信息
     *
     * @param info 设备产品信息
     */
    void update(DeviceProductInfo info);

    /**
     * @return 获取协议支持
     */
    ProtocolSupport getProtocol();

}
