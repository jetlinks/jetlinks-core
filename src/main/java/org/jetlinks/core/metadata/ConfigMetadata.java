package org.jetlinks.core.metadata;

import java.io.Serializable;
import java.util.List;

public interface ConfigMetadata extends Serializable {

    ConfigScope[] all = new ConfigScope[0];

    String getName();

    String getDescription();

    List<ConfigPropertyMetadata> getProperties();

    default ConfigScope[] getScope() {
        return all;
    }

    default boolean hasScope(ConfigScope target) {
        if (getScope() == all) {
            return true;
        }
        for (ConfigScope scope : getScope()) {
            if (scope.getId().equals(target.getId())) {
                return true;
            }
        }
        return false;
    }
}
