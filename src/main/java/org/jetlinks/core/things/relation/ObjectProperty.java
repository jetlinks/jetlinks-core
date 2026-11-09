package org.jetlinks.core.things.relation;

import org.jetlinks.core.config.ConfigKey;

import java.io.Serializable;
import java.util.Objects;

public interface ObjectProperty extends Serializable {

    /** 实际提供原值的逻辑来源；旧来源未声明时为 null，不推断为服务节点。 */
    default String getProviderId() {
        return null;
    }

    ConfigKey<String> name = ConfigKey.of("name");
    ConfigKey<String> description = ConfigKey.of("description");


    String getProperty();

    Object getValue();


    static ObjectProperty of(String property, Object value) {
        return SimpleObjectProperty
                .of(Objects.requireNonNull(property),
                    Objects.requireNonNull(value));
    }
}
