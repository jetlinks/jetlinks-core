package org.jetlinks.core.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Type;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
class SimpleConfigKey<V> implements ConfigKey<V> {

    private String key;

    private String name;

    private Type valueType;

}
