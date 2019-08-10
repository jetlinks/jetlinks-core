package org.jetlinks.core.metadata;

import org.jetlinks.core.metadata.unit.ValueUnit;

public interface UnitSupported {
    ValueUnit getUnit();

    void setUnit(ValueUnit unit);
}
