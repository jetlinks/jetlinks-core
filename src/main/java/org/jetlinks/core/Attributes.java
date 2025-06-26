package org.jetlinks.core;

import java.util.Optional;

public interface Attributes {

    static Attributes empty() {
        return EmptyAttributes.INSTANCE;
    }

    static Attributes create() {
        return new SimpleAttributes();
    }

    void setAttribute(Object key, Object value);

    <T> Optional<T> getAttribute(Object key);

    default <T> Optional<T> getAttribute(Class<T> key) {
        return getAttribute((Object) key).map(key::cast);
    }
}
