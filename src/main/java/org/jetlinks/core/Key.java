package org.jetlinks.core;

public interface Key<V> {
    String getKey();

    KeyValue<V> value(V value);
}
