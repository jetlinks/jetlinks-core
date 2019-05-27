package org.jetlinks.core.support.types;

import org.jetlinks.core.metadata.unit.MeasurementUnit;
import org.junit.Assert;
import org.junit.Test;

public class DoubleTypeTest {

    @Test
    public void test() {
        DoubleType type = new DoubleType();

        type.setMax(100D);
        type.setMin(0D);
        type.setScale(2);
        type.setUnit(JetlinksStandardValueUnit.of(MeasurementUnit.meter));

        Assert.assertTrue(type.validate(99.99991).isSuccess());

        Assert.assertFalse(type.validate(100.00001).isSuccess());
        Assert.assertFalse(type.validate(-0.00001).isSuccess());

        Assert.assertEquals(type.format(100.236), "100.24m");
        Assert.assertEquals(type.format(98.582), "98.58m");
        Assert.assertEquals(type.format(100), "100.00m");


        DoubleType type2 = new DoubleType();
        type2.fromJson(type.toJson());
        System.out.println(type2);
        Assert.assertEquals(type2.getMax(),type.getMax());
        Assert.assertEquals(type2.getMin(),type.getMin());
        Assert.assertEquals(type2.getScale(),type.getScale());

    }
}