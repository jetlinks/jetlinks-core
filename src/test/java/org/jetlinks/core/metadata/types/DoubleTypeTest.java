package org.jetlinks.core.metadata.types;

import org.junit.Assert;
import org.junit.Test;

public class DoubleTypeTest {

    @Test
    public void test() {
        DoubleType doubleType = new DoubleType();
        doubleType.setScale(2);

        Assert.assertEquals(doubleType.format(2.134D), "2.13");
        Assert.assertEquals(doubleType.format(2.135D), "2.14");

        Assert.assertEquals(doubleType.format("1.0"),"1.00");

        Assert.assertEquals(doubleType.format("a"),"a");


    }
}