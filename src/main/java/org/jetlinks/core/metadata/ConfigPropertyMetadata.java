package org.jetlinks.core.metadata;

import java.io.Serializable;

public interface ConfigPropertyMetadata extends ConfigScopeSupport,Serializable {

    String getProperty();

    String getName();

    String getDescription();

    DataType getType();
}
