package org.jetlinks.core;

import lombok.AllArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor(staticName = "of")
class SimpleValue implements Value, Serializable {

    private final Object nativeValue;

    @Override
    public Object get() {
        return nativeValue;
    }

}
