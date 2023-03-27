package org.jetlinks.core.command;

import java.io.Serializable;
import java.util.Map;

public interface Command<Response> extends Serializable {

    default Command<Response> with(String key, Object value) {
        return this;
    }

    default Command<Response> with(Map<String, Object> parameters) {
        return this;
    }

    default void validate() {

    }

}
