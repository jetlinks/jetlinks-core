package org.jetlinks.core.metadata;

import java.util.List;
import java.util.Optional;

/**
 * 服务元数据由: 属性，功能，事件组成
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface ServiceMetadata extends Metadata {

    List<PropertyMetadata> getProperties();

    List<FunctionMetadata> getFunctions();

    List<EventMetadata> getEvents();

    Optional<EventMetadata> getEvent(String name);

    Optional<PropertyMetadata> getProperty(String name);

    Optional<FunctionMetadata> getFunction(String name);
}
