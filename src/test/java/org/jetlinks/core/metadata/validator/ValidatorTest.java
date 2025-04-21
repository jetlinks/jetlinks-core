package org.jetlinks.core.metadata.validator;

import org.junit.Assert;
import org.junit.Test;

public class ValidatorTest {

    @Test
    public void test() {
        Validator validator = Validator.complexPasswordValidator(5);
        String easy = "123456";
        String overLength = "Jetlinks-pwd1";
        String illegal = "Jetlinks";
        String legal = "Jet-1";

        boolean easyResult = validator.validate(easy);
        boolean overLengthResult = validator.validate(overLength);
        boolean illegalResult = validator.validate(illegal);
        boolean legalResult = validator.validate(legal);

        Assert.assertFalse(easyResult);
        Assert.assertFalse(overLengthResult);
        Assert.assertFalse(illegalResult);
        Assert.assertTrue(legalResult);
    }
}
