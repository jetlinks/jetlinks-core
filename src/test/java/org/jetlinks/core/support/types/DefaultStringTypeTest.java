package org.jetlinks.core.support.types;

import org.jetlinks.core.metadata.unit.UnifyUnit;
import org.junit.Assert;
import org.junit.Test;

public class DefaultStringTypeTest {

    @Test
    public void test(){
        DefaultStringType type=new DefaultStringType();

        type.setUnit(JetLinksStandardValueUnit.of(UnifyUnit.meter));

        Assert.assertTrue(type.validate("123").isSuccess());
        Assert.assertFalse(type.validate(null).isSuccess());


        DefaultStringType type2=new DefaultStringType();
        type2.fromJson(type.toJson());


        Assert.assertEquals(type2.format("1"),"1m");

    }

}