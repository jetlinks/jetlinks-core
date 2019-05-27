package org.jetlinks.core.support.types;

import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.core.metadata.ValidateResult;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class UnknownType implements DataType {
    @Override
    public ValidateResult validate(Object value) {
        return ValidateResult.success();
    }

    @Override
    public ValueUnit getUnit() {
        return null;
    }

    @Override
    public String getId() {
        return "unknown";
    }

    @Override
    public String getName() {
        return "未知类型";
    }

    @Override
    public String getDescription() {
        return "未知类型";
    }

}
