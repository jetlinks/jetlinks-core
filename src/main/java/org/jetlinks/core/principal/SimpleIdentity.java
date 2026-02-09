package org.jetlinks.core.principal;

import org.jspecify.annotations.NonNull;

record SimpleIdentity(String type, String identifier) implements Identity {

    @Override
    public @NonNull String getType() {
        return type;
    }


    @Override
    public @NonNull String getIdentifier() {
        return identifier;
    }
}
