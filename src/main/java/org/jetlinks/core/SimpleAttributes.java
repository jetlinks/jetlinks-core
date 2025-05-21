package org.jetlinks.core;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class SimpleAttributes extends ConcurrentHashMap<Object, Object> implements Attributes {
    @Override
    public void setAttribute(Object key, Object value) {
        put(key, value);
    }

    @Override
    @SuppressWarnings("all")
    public <T> Optional<T> getAttribute(Object key) {
        return Optional.ofNullable((T) get(key));
    }
}
