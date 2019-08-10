package org.jetlinks.core.metadata.types;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class NumberTypeTest {

    @Test
    public void testInt() {

        NumberType type = new IntType();
        type.setMin(1);
        type.setMax(3);

        Assert.assertFalse(type.validate(0).isSuccess());
        Assert.assertTrue(type.validate(1).isSuccess());
        Assert.assertTrue(type.validate(2).isSuccess());
        Assert.assertTrue(type.validate(3).isSuccess());
        Assert.assertFalse(type.validate(4).isSuccess());

    }

    @Test
    public void testDouble() {

        NumberType type = new DoubleType();
        type.setMin(1);
        type.setMax(99.99);

        Assert.assertFalse(type.validate(0).isSuccess());
        Assert.assertTrue(type.validate(1).isSuccess());
        Assert.assertTrue(type.validate(2).isSuccess());
        Assert.assertTrue(type.validate(99.99).isSuccess());
        Assert.assertFalse(type.validate(99.999).isSuccess());

    }



}