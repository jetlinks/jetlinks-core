package org.jetlinks.core.metadata.types;

import org.junit.Assert;
import org.junit.Test;

public class NumberTypeTest {

    @Test
    public void testInt() {

        IntType type = new IntType();
        type.setMin(1);
        type.setMax(3);

        Assert.assertFalse(type.validate(0).isSuccess());
        Assert.assertTrue(type.validate(1).isSuccess());
        Assert.assertTrue(type.validate(2).isSuccess());
        Assert.assertTrue(type.validate(3).isSuccess());
        Assert.assertFalse(type.validate(4).isSuccess());

        Assert.assertEquals(Integer.valueOf(100), type.convertScaleNumber(100));
        Assert.assertEquals(Integer.valueOf(100), type.convertScaleNumber(99.99));

    }

    @Test
    public void testDouble() {

        DoubleType type = new DoubleType();
        type.setMin(1);
        type.setMax(99.99);
        type.setScale(2);

        Assert.assertFalse(type.validate(0).isSuccess());
        Assert.assertTrue(type.validate(1).isSuccess());
        Assert.assertTrue(type.validate(2).isSuccess());
        Assert.assertTrue(type.validate(99.99).isSuccess());
        Assert.assertFalse(type.validate(99.999).isSuccess());

        Assert.assertEquals(new Double(99.99D), type.convertScaleNumber(99.991));

    }

    static {
        System.setProperty("jetlinks.type.number.convert.stripTrailingZeros", "true");
    }

    @Test
    public void testFloat() {


        FloatType type = new FloatType();
        type.setMin(1);
        type.setMax(99.99);
        type.setScale(2);

        System.out.println(type.format(1.3));
        Assert.assertFalse(type.validate(0).isSuccess());
        Assert.assertTrue(type.validate(1).isSuccess());
        Assert.assertTrue(type.validate(2).isSuccess());
        Assert.assertTrue(type.validate(99.99).isSuccess());
        Assert.assertFalse(type.validate(99.999).isSuccess());

        Assert.assertEquals(new Float(99.99F), type.convertScaleNumber(99.991));


        Assert.assertEquals(Float.valueOf(Float.MAX_VALUE), type.convert(Float.MAX_VALUE));
        Assert.assertEquals(Float.valueOf(Float.MIN_VALUE), type.convert(Float.MIN_VALUE));

    }


}