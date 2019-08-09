package org.jetlinks.core.support.types;

import org.jetlinks.core.metadata.unit.UnifyUnit;
import org.junit.Assert;
import org.junit.Test;

public class DefaultDoubleTypeTest {

    @Test
    public void test() {
        DefaultDoubleType type = new DefaultDoubleType();

        type.setMax(100D);
        type.setMin(0D);
        type.setScale(2);
        type.setUnit(JetLinksStandardValueUnit.of(UnifyUnit.meter));

        Assert.assertTrue(type.validate(99.99991).isSuccess());

        Assert.assertFalse(type.validate(100.00001).isSuccess());
        Assert.assertFalse(type.validate(-0.00001).isSuccess());

        Assert.assertEquals(type.format(100.236), "100.24m");
        Assert.assertEquals(type.format(98.582), "98.58m");
        Assert.assertEquals(type.format(100), "100.00m");


        DefaultDoubleType type2 = new DefaultDoubleType();
        type2.fromJson(type.toJson());
        System.out.println(type2);
        Assert.assertEquals(type2.getMax(),type.getMax());
        Assert.assertEquals(type2.getMin(),type.getMin());
        Assert.assertEquals(type2.getScale(),type.getScale());

    }
}