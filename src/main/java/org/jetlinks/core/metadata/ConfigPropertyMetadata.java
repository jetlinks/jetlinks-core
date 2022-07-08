package org.jetlinks.core.metadata;

import java.io.Serializable;
import java.util.Map;

public interface ConfigPropertyMetadata extends ConfigScopeSupport,Serializable {

    String getProperty();

    String getName();

    String getDescription();

    DataType getType();

    Map<String,Object> getExpands();

}
