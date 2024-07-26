package org.jetlinks.core.metadata.types;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

public class EnumTypeTest {

    @Test
    public void test(){

        EnumType type=new EnumType();

        Assert.assertFalse(type.validate("1").isSuccess());
        Assert.assertEquals(type.format("1"),"1");

        type.addElement(EnumType.Element.of("1","男"));
        type.addElement(EnumType.Element.of("2","女"));

        Assert.assertTrue(type.validate("1").isSuccess());
        Assert.assertEquals(type.format("1"),"男");


    }

    @Test
    public void testMulti(){
        EnumType type=new EnumType();
        type.setMulti(true);

        type.addElement(EnumType.Element.of("1","男"));
        type.addElement(EnumType.Element.of("2","女"));

        Assert.assertTrue(type.validate("1,2").isSuccess());
        Assert.assertEquals(type.format("1,2"), "男,女");
        Assert.assertEquals(type.format(Lists.newArrayList("1","2")), Lists.newArrayList("男","女"));

        Assert.assertFalse(type.validate("3").isSuccess());
        Assert.assertTrue(type.validate("1,3").isSuccess());
        Assert.assertFalse(type.validate("3,4").isSuccess());
        Assert.assertEquals(type.format("1,3"), "男,3");
        Assert.assertEquals(type.format(Lists.newArrayList("1","3")), Lists.newArrayList("男","3"));
    }

}