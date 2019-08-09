package org.jetlinks.core.support.types;

import org.jetlinks.core.metadata.unit.UnifyUnit;
import org.junit.Assert;
import org.junit.Test;

public class DefaultIntTypeTest {

    @Test
    public void test(){
        DefaultIntType type=new DefaultIntType();

        type.setMax(100);
        type.setMin(0);
        type.setUnit(JetLinksStandardValueUnit.of(UnifyUnit.meter));

        Assert.assertTrue(type.validate(20).isSuccess());
        Assert.assertFalse(type.validate(101).isSuccess());
        Assert.assertFalse(type.validate(-1).isSuccess());


        DefaultIntType type2=new DefaultIntType();
        type2.fromJson(type.toJson());
        System.out.println(type2);

        Assert.assertEquals(type.getMax(),type2.getMax());
        Assert.assertEquals(type.getMin(),type2.getMin());


        Assert.assertEquals(type2.getUnit().format(10),"10m");

    }

}