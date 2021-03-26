package org.jetlinks.core.metadata;

import java.util.List;
import java.util.Optional;

/**
 * 物模型定义
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMetadata extends Metadata, Jsonable {

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

    default Optional<EventMetadata> getEvent(String id) {
        return Optional.ofNullable(getEventOrNull(id));
    }

    EventMetadata getEventOrNull(String id);

    default Optional<PropertyMetadata> getProperty(String id) {
        return Optional.ofNullable(getPropertyOrNull(id));
    }

    PropertyMetadata getPropertyOrNull(String id);

    default Optional<FunctionMetadata> getFunction(String id) {
        return Optional.ofNullable(getFunctionOrNull(id));
    }

    FunctionMetadata getFunctionOrNull(String id);

    default Optional<PropertyMetadata> getTag(String id) {
        return Optional.ofNullable(getTagOrNull(id));
    }

    PropertyMetadata getTagOrNull(String id);

    /**
     * 合并物模型，合并后返回新的物模型对象
     *
     * @param metadata 要合并的物模型
     * @since 1.8
     */
    default DeviceMetadata merge(DeviceMetadata metadata) {
        return merge(metadata, MergeOption.DEFAULT_OPTIONS);
    }

    default DeviceMetadata merge(DeviceMetadata metadata, MergeOption... options) {
        throw new UnsupportedOperationException("unsupported merge metadata");
    }
}
