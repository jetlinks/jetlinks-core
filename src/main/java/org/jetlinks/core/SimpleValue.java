package org.jetlinks.core;

import lombok.AllArgsConstructor;

@AllArgsConstructor(staticName = "of")
class SimpleValue implements Value {

    private Object nativeValue;

    @Override
    public Object get() {
        return nativeValue;
    }

    @Override
    public <T> T as(Class<T> type) {
        // TODO: 2019-10-19 不同类型转换支持
        return type.cast(nativeValue);
    }
}
