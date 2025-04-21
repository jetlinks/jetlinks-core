package org.jetlinks.core.metadata.validator;

import org.jetlinks.core.metadata.ValidateResult;
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
    protected ValidateResult doValidate(Object value) {
        if (pattern.matcher(String.valueOf(value)).matches()) {
            return ValidateResult.success(value);
        }
        return ValidateResult.fail();
    }
}
