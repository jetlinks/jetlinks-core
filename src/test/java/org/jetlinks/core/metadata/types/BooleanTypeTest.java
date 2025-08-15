package org.jetlinks.core.metadata.types;

import org.jetlinks.core.metadata.Jsonable;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BooleanTypeTest extends JsonableTestBase<BooleanType>{

    @Test
    public void test(){
        BooleanType booleanType=new BooleanType();

        assertEquals(booleanType.format(true),"是");
        assertEquals(booleanType.format(false),"否");

        Assert.assertTrue(booleanType.validate(false).isSuccess());
        Assert.assertTrue(booleanType.validate(true).isSuccess());

        Assert.assertTrue(booleanType.validate("false").isSuccess());
        Assert.assertTrue(booleanType.validate("true").isSuccess());

        Assert.assertTrue(booleanType.validate("是").isSuccess());
        Assert.assertTrue(booleanType.validate("否").isSuccess());

        Assert.assertTrue(booleanType.convert("1"));
        Assert.assertFalse(booleanType.convert("2"));

    }

    @Override
    protected BooleanType newInstance() {
        return new BooleanType();
    }

    @Override
    protected void fillSampleData(BooleanType instance) {
        instance.trueText("是的");
        instance.falseText("不是");
        instance.trueValue("T");
        instance.falseValue("F");
    }

    @Override
    protected void assertSampleData(BooleanType instance) {
        assertEquals("是的", instance.getTrueText());
        assertEquals("不是", instance.getFalseText());
        assertEquals("T", instance.getTrueValue());
        assertEquals("F", instance.getFalseValue());
    }
}