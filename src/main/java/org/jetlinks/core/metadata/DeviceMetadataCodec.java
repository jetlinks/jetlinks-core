package org.jetlinks.core.metadata;

import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * 物模型编解码器,用于将物模型与字符串进行互相转换
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMetadataCodec {

    /**
     * @return 物模型标识
     */
    default String getId() {
        return this.getClass().getSimpleName();
    }

    /**
     * @return 物模型名称
     */
    default String getName() {
        return getId();
    }

    /**
     * 将数据解码为物模型
     *
     * @param source 数据
     * @return 物模型
     */
    Mono<DeviceMetadata> decode(String source);

    /**
     * 将物模型编码为字符串
     *
     * @param metadata 物模型
     * @return 物模型字符串
     */
    Mono<String> encode(DeviceMetadata metadata);

    /**
     * 获取拓展配置元数据定义
     *
     * @param metadataType 物模型类型
     * @param dataTypeId   类型ID {@link DataType#getId()}
     * @return 配置定义
     * @since 1.1.4
     */
    default List<ConfigPropertyMetadata> getExpandsConfigMetadata(DeviceMetadataType metadataType,
                                                                  String dataTypeId) {
        return Collections.emptyList();
    }

}
