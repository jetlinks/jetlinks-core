package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class BooleanTypeTest {

    @Test
    public void test() {

        BooleanType type = new BooleanType();
        System.out.println(type);
        Assert.assertTrue(type.validate(true).isSuccess());
        Assert.assertTrue(type.validate(false).isSuccess());
        Assert.assertTrue(type.validate("true").isSuccess());
        Assert.assertTrue(type.validate("false").isSuccess());
        Assert.assertTrue(type.validate("是").isSuccess());
        Assert.assertTrue(type.validate("否").isSuccess());
        Assert.assertFalse(type.validate("0").isSuccess());
        Assert.assertFalse(type.validate("1").isSuccess());


        Assert.assertEquals(type.getUnit().format("true"), "是");
        Assert.assertEquals(type.getUnit().format("false"), "否");
        Assert.assertEquals(type.getUnit().format(true), "是");
        Assert.assertEquals(type.getUnit().format(false), "否");

        type.setTrueText("开启");
        type.setFalseText("关闭");

        type.setTrueValue("1");
        type.setFalseValue("0");

        Assert.assertEquals(type.getUnit().format("1"), "开启");
        Assert.assertEquals(type.getUnit().format("0"), "关闭");
        Assert.assertEquals(type.getUnit().format(1), "开启");
        Assert.assertEquals(type.getUnit().format(0), "关闭");

        JSONObject jsonObject= type.toJson();
        BooleanType type2=new BooleanType();
        type2.fromJson(jsonObject);

        Assert.assertEquals(type.getTrueText(),type2.getTrueText());
        Assert.assertEquals(type.getTrueValue(),type2.getTrueValue());
        Assert.assertEquals(type.getFalseValue(),type2.getFalseValue());
        Assert.assertEquals(type.getFalseText(),type2.getFalseText());
        Assert.assertEquals(type.getId(),type2.getId());
        Assert.assertEquals(type.getName(),type2.getName());

    }

}