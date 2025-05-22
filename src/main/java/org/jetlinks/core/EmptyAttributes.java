package org.jetlinks.core;

import java.util.Optional;

class EmptyAttributes implements Attributes{
    static final EmptyAttributes INSTANCE = new EmptyAttributes();

    @Override
    public void setAttribute(Object key, Object value) {

    }

    @Override
    public <T> Optional<T> getAttribute(Object key) {
        return Optional.empty();
    }
}
