package org.jetlinks.core.things.relation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SimpleObjectProperty implements ObjectProperty {
    private String property;
    private Object value;
}
