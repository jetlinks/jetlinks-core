package org.jetlinks.core.metadata;

import java.util.Map;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DataType extends Metadata, FormatSupport {

    ValidateResult validate(Object value);

    default String getType() {
        return getId();
    }

    @Override
    default Map<String, Object> getExpands() {
        return null;
    }

}
