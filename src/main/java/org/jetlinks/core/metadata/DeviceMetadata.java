package org.jetlinks.core.metadata;

import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.things.ThingMetadata;

import java.util.List;

/**
 * 设备物模型定义
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMetadata extends ThingMetadata {

    /**
     * @return 所有属性定义
     * @see org.jetlinks.core.message.property.ReadPropertyMessage
     * @see org.jetlinks.core.message.property.WritePropertyMessage
     * @see org.jetlinks.core.message.property.ReportPropertyMessage
     * @see org.jetlinks.core.message.property.ReadPropertyMessageReply
     * @see org.jetlinks.core.message.property.WritePropertyMessageReply
     */
    List<PropertyMetadata> getProperties();

    /**
     * @return 所有功能定义
     * @see org.jetlinks.core.message.function.FunctionInvokeMessage
     * @see org.jetlinks.core.message.function.FunctionInvokeMessageReply
     */
    List<FunctionMetadata> getFunctions();

    /**
     * @return 事件定义
     * @see org.jetlinks.core.message.event.EventMessage
     */
    List<EventMetadata> getEvents();

    /**
     * @return 标签定义
     * @see org.jetlinks.core.message.UpdateTagMessage
     */
    List<PropertyMetadata> getTags();

    /**
     * 合并物模型，合并后返回新的物模型对象
     *
     * @param metadata 要合并的物模型
     * @since 1.1.6
     */
    @Override
    default <T extends ThingMetadata> DeviceMetadata merge(T metadata) {
        return this.merge(metadata,MergeOption.DEFAULT_OPTIONS);
    }

    default  <T extends ThingMetadata> DeviceMetadata merge(T metadata, MergeOption... options) {
        throw new UnsupportedOperationException("unsupported merge metadata");
    }

    @Override
    default DeviceMetadata expand(String key, Object value) {
          ThingMetadata.super.expand(key, value);
          return this;
    }

    @Override
    default <T> DeviceMetadata expand(ConfigKey<T> key, T value) {
          ThingMetadata.super.expand(key, value);
          return this;
    }
}
