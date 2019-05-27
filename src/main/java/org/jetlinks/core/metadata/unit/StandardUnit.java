package org.jetlinks.core.metadata.unit;

import java.io.Serializable;

/**
 * 标准单位
 *
 * @see MeasurementUnit
 * @since 1.0.0
 */
public interface StandardUnit extends Serializable {

    String getId();

    String getType();

    String getName();

    String getDescription();

    String format(Object value);

}