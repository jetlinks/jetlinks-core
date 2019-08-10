package org.jetlinks.core.metadata.unit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StandardValueUnit implements ValueUnit {

    private StandardUnit standardUnit;

    @Override
    public String getSymbol() {
        return standardUnit.getSymbol();
    }

    @Override
    public String format(Object value) {
        return standardUnit.format(value);
    }

    @Override
    public String getId() {
        return standardUnit.getId();
    }

    @Override
    public String getName() {
        return standardUnit.getName();
    }

    @Override
    public String getDescription() {
        return standardUnit.getDescription();
    }


}
