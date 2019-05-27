package org.jetlinks.core.support.types;

import org.jetlinks.core.metadata.unit.MeasurementUnit;
import org.junit.Assert;
import org.junit.Test;

public class StringTypeTest {

    @Test
    public void test(){
        StringType type=new StringType();

        type.setUnit(JetlinksStandardValueUnit.of(MeasurementUnit.meter));

        Assert.assertTrue(type.validate("123").isSuccess());
        Assert.assertFalse(type.validate(null).isSuccess());


        StringType type2=new StringType();
        type2.fromJson(type.toJson());


        Assert.assertEquals(type2.format("1"),"1m");

    }

}