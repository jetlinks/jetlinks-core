package org.jetlinks.core.metadata.types;

import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class UnknownType implements DataType {
    public static final UnknownType GLOBAL = new UnknownType();
    @Override
    public ValidateResult validate(Object value) {
        return ValidateResult.success();
    }

    @Override
    public String getId() {
        return "unknown";
    }

    @Override
    public String getName() {
        return LocaleUtils.resolveMessage("message.metadata.type.unknown", LocaleUtils.current(), "未知类型");
    }

    @Override
    public String getDescription() {
        return "未知类型";
    }

    @Override
    public String format(Object value) {
        return String.valueOf(value);
    }
}
