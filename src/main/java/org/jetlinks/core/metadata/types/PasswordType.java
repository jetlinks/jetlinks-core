package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;

@Getter
@Setter
public class PasswordType extends AbstractType<PasswordType> implements DataType, Converter<String> {
    public static final String ID = "password";
    public static final PasswordType GLOBAL = new PasswordType();

    private int minLength = 8;

    private int maxLength = 64;

    private int level = 2;

    public PasswordType minLength(int minLength) {
        this.minLength = minLength;
        return this;
    }

    public PasswordType maxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public PasswordType level(int level) {
        this.level = level;
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
        return ValidateResult.success(String.valueOf(value));
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
