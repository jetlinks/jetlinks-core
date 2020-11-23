package org.jetlinks.core.defaults;

import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceMetadataType;
import reactor.core.publisher.Flux;

public interface ExpandsConfigMetadataSupplier {


    static StaticExpandsConfigMetadataSupplier create() {
        return new StaticExpandsConfigMetadataSupplier();
    }


    /**
     * 获取物模型拓展配置信息
     *
     * @param metadataType 物模型类型
     * @param metadataId   物模型标识
     * @param dataTypeId   数据类型ID
     * @return 配置信息
     */
    Flux<ConfigMetadata> getConfigMetadata(DeviceMetadataType metadataType,
                                           String metadataId,
                                           String dataTypeId);


}
