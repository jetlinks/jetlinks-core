package org.jetlinks.core.device.identity;

import reactor.core.publisher.Mono;

public interface DeviceIdentityManager {

    /**
     * 根据设备身份信息获取平台内部设备ID
     *
     * @param identity 身份信息
     * @return 设备ID
     */
    Mono<String> findDevice(Identity identity);


}
