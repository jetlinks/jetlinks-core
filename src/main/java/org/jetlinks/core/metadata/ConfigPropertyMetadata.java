package org.jetlinks.core.metadata;

import java.io.Serializable;

public interface ConfigPropertyMetadata extends Serializable {

    String getProperty();

    String getName();

    String getDescription();

    DataType getType();
}
