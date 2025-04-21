package org.jetlinks.core.metadata.validator;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.regex.Pattern;

public class RegexValidator implements Validator {

    private final Pattern pattern;

    public RegexValidator(String regex) {
        Assert.isTrue(StringUtils.hasText(regex), "regex cannot be empty");
        this.pattern = Pattern.compile(regex);
    }

    @Override
    public boolean validate(Object value) {
        if (Objects.nonNull(value)) {
            return pattern.matcher(String.valueOf(value)).matches();
        }
        return false;
    }
}
