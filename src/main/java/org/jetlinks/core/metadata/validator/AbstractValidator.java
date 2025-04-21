package org.jetlinks.core.metadata.validator;

import org.jetlinks.core.metadata.ValidateResult;
import org.springframework.util.StringUtils;

import java.util.Objects;

public abstract class AbstractValidator implements Validator {

    private final String errorMessage;

    protected AbstractValidator(String errorMessage) {
        if (StringUtils.hasText(errorMessage)) {
            this.errorMessage = errorMessage;
        } else {
            this.errorMessage = defaultErrorMessage();
        }

    }

    /**
     * 校验器默认错误信息
     *
     * @return String
     */
    protected abstract String defaultErrorMessage();

    @Override
    public ValidateResult validate(Object value) {
        if (Objects.nonNull(value)) {
            if (!doValidate(value)) {
                return ValidateResult.fail(errorMessage);
            }
            return ValidateResult.success(value);
        }
        return ValidateResult.fail(errorMessage);
    }

    protected abstract boolean doValidate(Object value);
}
