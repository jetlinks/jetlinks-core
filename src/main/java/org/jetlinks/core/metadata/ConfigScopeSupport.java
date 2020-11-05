package org.jetlinks.core.metadata;

public interface ConfigScopeSupport {
    ConfigScope[] all = new ConfigScope[0];

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
