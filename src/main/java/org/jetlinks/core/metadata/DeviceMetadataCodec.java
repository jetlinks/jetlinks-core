package org.jetlinks.core.metadata;

import reactor.core.publisher.Mono;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMetadataCodec {

    /**
     * 解码 将数据解码为协议元数据
     * @param source 数据
     * @return 元数据
     */
    Mono<DeviceMetadata> decode(String source);

    /**
     * 编码 将元数据编码为string字符串
     * @param metadata 元数据
     * @return 数据
     */
    Mono<String> encode(DeviceMetadata metadata);

}
