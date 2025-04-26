package org.jetlinks.core.metadata.validator;

import org.jetlinks.core.metadata.ValidateResult;
import org.junit.Assert;
import org.junit.Test;

public class ValidatorTest {

    @Test
    public void test() {
        Validator validator = Validator.complexPasswordValidator(5, "password validate failed");
        String easy = "123456";
        String overLength = "Jetlinks-pwd1";
        String illegal = "Jetlinks";
        String legal = "Jet-1";

        ValidateResult easyResult = validator.validate(easy);
        ValidateResult overLengthResult = validator.validate(overLength);
        ValidateResult illegalResult = validator.validate(illegal);
        ValidateResult legalResult = validator.validate(legal);

        Assert.assertFalse(easyResult.isSuccess());
        Assert.assertFalse(overLengthResult.isSuccess());
        Assert.assertFalse(illegalResult.isSuccess());
        Assert.assertTrue(legalResult.isSuccess());
    }
}
