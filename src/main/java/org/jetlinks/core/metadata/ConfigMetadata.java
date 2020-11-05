package org.jetlinks.core.metadata;

import java.io.Serializable;
import java.util.List;

public interface ConfigMetadata extends ConfigScopeSupport, Serializable {

    String getName();

    String getDescription();

    List<ConfigPropertyMetadata> getProperties();

}
