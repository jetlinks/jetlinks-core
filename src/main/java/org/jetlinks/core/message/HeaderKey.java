package org.jetlinks.core.message;

public interface HeaderKey<T> {

    String getKey();

    T getDefaultValue();

    static <T> HeaderKey<T> of(String key, T defaultValue) {
        return new HeaderKey<T>() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public T getDefaultValue() {
                return defaultValue;
            }
        };
    }
}
