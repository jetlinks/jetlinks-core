package org.jetlinks.core.metadata;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface PropertyMetadata extends Metadata, Jsonable {

    DataType getValueType();

}
