package org.jetlinks.core.defaults;

import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceMetadataType;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StaticExpandsConfigMetadataSupplier implements ExpandsConfigMetadataSupplier {

    private final Map<String, List<ConfigMetadata>> metadata = new ConcurrentHashMap<>();

    private List<ConfigMetadata> getOrCreateConfigs(String id) {
        return metadata.computeIfAbsent(id, ignore -> new ArrayList<>());
    }

    /**
     * 添加配置,所有物模型都有此配置
     *
     * @param configMetadata 配置信息
     * @return this
     */
    public StaticExpandsConfigMetadataSupplier addConfigMetadata(ConfigMetadata configMetadata) {
        getOrCreateConfigs("any:any").add(configMetadata);
        return this;
    }

    /**
     * 添加通用配置,根据类型来指定配置
     *
     * @param typeId         类型ID
     * @param configMetadata 配置
     * @return this
     */
    public StaticExpandsConfigMetadataSupplier addConfigMetadata(String typeId,
                                                                 ConfigMetadata configMetadata) {

        getOrCreateConfigs(String.join(":", "any", typeId)).add(configMetadata);

        return this;
    }

    /**
     * 添加通用配置,指定都物模型都使用指定都配置
     *
     * @param metadataType   物模型类型
     * @param configMetadata 配置
     * @return this
     */
    public StaticExpandsConfigMetadataSupplier addConfigMetadata(DeviceMetadataType metadataType,
                                                                 ConfigMetadata configMetadata) {

        return addConfigMetadata(metadataType, "any", configMetadata);
    }

    /**
     * 添加通用配置,指定都物模型以及数据类型使用指定的配置
     *
     * @param metadataType   物模型类型
     * @param configMetadata 配置
     * @return this
     */
    public StaticExpandsConfigMetadataSupplier addConfigMetadata(DeviceMetadataType metadataType,
                                                                 String typeId,
                                                                 ConfigMetadata configMetadata) {
        getOrCreateConfigs(String.join(":", metadataType.name(), typeId)).add(configMetadata);
        return this;
    }

    @Override
    public Flux<ConfigMetadata> getConfigMetadata(DeviceMetadataType metadataType,
                                                  String metadataId,
                                                  String dataTypeId) {
        return Flux.merge(
                Flux.fromIterable(metadata.getOrDefault("any:any", Collections.emptyList())),
                Flux.fromIterable(metadata.getOrDefault(String.join(":", "any", dataTypeId), Collections.emptyList())),
                Flux.fromIterable(metadata.getOrDefault(String.join(":", metadataType.name(), "any"), Collections.emptyList())),
                Flux.fromIterable(metadata.getOrDefault(String.join(":", metadataType.name(), dataTypeId), Collections.emptyList()))
        );
    }
}
