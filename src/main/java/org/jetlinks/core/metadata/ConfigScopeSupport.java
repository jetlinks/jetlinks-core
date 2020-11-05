package org.jetlinks.core.metadata;

import java.util.Arrays;

public interface ConfigScopeSupport {
    ConfigScope[] all = new ConfigScope[0];

    default ConfigScope[] getScopes() {
        return all;
    }

    default boolean hasAnyScope(ConfigScope... target) {
        if (target.length == 0 || getScopes() == all) {
            return true;
        }
        return Arrays
                .stream(target)
                .anyMatch(this::hasScope);
    }

    default boolean hasScope(ConfigScope target) {
        if (getScopes() == all) {
            return true;
        }
        for (ConfigScope scope : getScopes()) {
            if (scope.getId().equals(target.getId())) {
                return true;
            }
        }
        return false;
    }
}
