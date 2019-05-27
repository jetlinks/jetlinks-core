package org.jetlinks.core.metadata;

import org.jetlinks.core.metadata.unit.ValueUnit;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DataType extends Metadata {

    ValidateResult validate(Object value);

    ValueUnit getUnit();

    default String format(Object value) {
        ValueUnit unit = getUnit();
        if (unit == null) {
            return String.valueOf(value);
        }
        return unit.format(value);
    }
}
