package org.jetlinks.core.message;

public interface HeaderKey<T> {

    String getKey();

    T getDefaultValue();

    @SuppressWarnings("all")
    default Class<T> getType() {
        return getDefaultValue() == null ? (Class<T>) Object.class : (Class<T>) getDefaultValue().getClass();
    }

    static <T> HeaderKey<T> of(String key, T defaultValue, Class<T> type) {
        return new HeaderKey<T>() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public T getDefaultValue() {
                return defaultValue;
            }

            @Override
            public Class<T> getType() {
                return type;
            }
        };
    }

    @SuppressWarnings("all")
    static <T> HeaderKey<T> of(String key, T defaultValue) {
        return of(key, defaultValue, defaultValue == null ? (Class<T>) Object.class : (Class<T>) defaultValue.getClass());
    }
}
