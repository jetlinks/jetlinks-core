package org.jetlinks.core;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
class SimpleValue implements Value, Serializable {

    private final Object nativeValue;

    @Override
    public Object get() {
        return nativeValue;
    }

    @Override
    public String toString() {
        return String.valueOf(nativeValue);
    }
}
