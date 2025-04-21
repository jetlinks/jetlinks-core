package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;
import org.jetlinks.core.metadata.validator.Validator;

import java.util.Optional;

@Getter
@Setter
public class PasswordType extends AbstractType<PasswordType> implements DataType, Converter<String> {
    public static final String ID = "password";
    public static final PasswordType GLOBAL = new PasswordType();

    private Validator validator;

    /**
     * 添加校验器
     *
     * @param validator 校验器
     * @return PasswordType
     */
    public PasswordType withValidator(Validator validator) {
        this.validator = validator;
        return this;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return LocaleUtils.resolveMessage("message.metadata.type.password", LocaleUtils.current(), "密码");
    }

    @Override
    public ValidateResult validate(Object value) {
        return Optional
            .ofNullable(validator)
            .map(validator -> {
                if (validator.validate(value)) {
                    return ValidateResult.success(String.valueOf(value));
                }
                return ValidateResult.fail(String.valueOf(value));
            })
            .orElse(ValidateResult.success(String.valueOf(value)));

    }

    @Override
    public String format(Object value) {
        return String.valueOf(value);
    }

    @Override
    public String convert(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
