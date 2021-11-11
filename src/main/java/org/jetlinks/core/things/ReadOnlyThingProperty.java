package org.jetlinks.core.things;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
class ReadOnlyThingProperty implements ThingProperty {

    private final String property;

    private final Object value;

    private final long timestamp;

    private final String state;
}
