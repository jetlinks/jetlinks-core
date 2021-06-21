package org.jetlinks.core.message.property;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SimplePropertyValue implements Property {
    private String id;
    private Object value;
    private long timestamp;
    private String state;
}
