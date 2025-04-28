package org.jetlinks.core.metadata.validator;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class RegexValidator extends AbstractValidator {

    private final Pattern pattern;

    public RegexValidator(String regex, String errorMessage) {
        super(errorMessage);
        Assert.isTrue(StringUtils.hasText(regex), "regex cannot be empty");
        this.pattern = Pattern.compile(regex);
    }

    @Override
    protected String defaultErrorMessage() {
        return "message.regex_validator_fail";
    }

    @Override
    protected boolean doValidate(Object value) {
        return pattern.matcher(String.valueOf(value)).matches();
    }
}
