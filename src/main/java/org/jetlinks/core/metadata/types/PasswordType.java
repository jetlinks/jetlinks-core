package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;

import java.util.Map;

@Getter
@Setter
public class PasswordType implements DataType, Converter<String> {
    public static final String ID = "password";

    private String description;

    private Map<String, Object> expands;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "密码";
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
