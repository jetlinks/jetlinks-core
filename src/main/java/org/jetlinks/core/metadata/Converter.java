package org.jetlinks.core.metadata;

public interface Converter<T> {

    T convert(Object value);
}
