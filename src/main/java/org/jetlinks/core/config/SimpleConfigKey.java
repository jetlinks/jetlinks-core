package org.jetlinks.core.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
class SimpleConfigKey<V> implements ConfigKey<V> {

    private String key;

    private String name;

    private Class<V> type;

}
