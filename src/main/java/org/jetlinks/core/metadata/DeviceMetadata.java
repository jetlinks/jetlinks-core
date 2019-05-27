package org.jetlinks.core.metadata;

import java.util.List;
import java.util.Optional;

/**
 * 设备元数据由: 属性，功能，事件组成
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMetadata extends Metadata ,Jsonable {

    List<PropertyMetadata> getProperties();

    List<FunctionMetadata> getFunctions();

    List<EventMetadata> getEvents();

    Optional<EventMetadata> getEvent(String id);

    Optional<PropertyMetadata> getProperty(String id);

    Optional<FunctionMetadata> getFunction(String id);

}
