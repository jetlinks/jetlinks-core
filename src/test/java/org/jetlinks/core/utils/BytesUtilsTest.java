package org.jetlinks.core.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class BytesUtilsTest {

    @Test
    public void testInt() {
        Assert.assertEquals(BytesUtils.highBytesToInt(BytesUtils.toHighBytes(32)), 32);
        Assert.assertEquals(BytesUtils.lowBytesToInt(BytesUtils.toLowBytes(32)), 32);

        Assert.assertEquals(BytesUtils.lowBytesToInt(BytesUtils.toLowBytes(Short.MIN_VALUE)), Short.MIN_VALUE);
        Assert.assertEquals(BytesUtils.lowBytesToInt(BytesUtils.toLowBytes(Integer.MIN_VALUE)), Integer.MIN_VALUE);
        Assert.assertEquals(BytesUtils.lowBytesToInt(BytesUtils.toLowBytes(Integer.MAX_VALUE)), Integer.MAX_VALUE);

    }

    @Test
    public void testLong() {
        long val = Integer.MAX_VALUE + 100L;

        Assert.assertEquals(BytesUtils.highBytesToLong(BytesUtils.toHighBytes(val)), val);
        Assert.assertEquals(BytesUtils.lowBytesToLong(BytesUtils.toLowBytes(val)), val);

    }

    @Test
    public void testDouble() {
        double val = new Random().nextDouble();
        Assert.assertEquals(BytesUtils.highBytesToDouble(BytesUtils.toHighBytes(val)), val, 0);
        Assert.assertEquals(BytesUtils.lowBytesToDouble(BytesUtils.toLowBytes(val)), val, 0);
    }

    @Test
    public void testFloat() {
        float val = new Random().nextFloat();
        Assert.assertEquals(BytesUtils.highBytesToFloat(BytesUtils.toHighBytes(val)), val, 0);
        Assert.assertEquals(BytesUtils.lowBytesToFloat(BytesUtils.toLowBytes(val)), val, 0);
    }

    @Test
    public void testOffset() {

        byte[] data = BytesUtils.toLowBytes(new byte[4], 566, 2, 2);

        Assert.assertEquals(BytesUtils.lowBytesToInt(data, 2, 2), 566);

        Assert.assertEquals(BytesUtils.lowBytesToInt(new byte[]{2, 1, 0, 2}, 1, 2), 256);

        Assert.assertEquals(BytesUtils.lowBytesToInt(new byte[]{0, 1, 2, 2}, 0, 2), 1);

        Assert.assertEquals(BytesUtils.highBytesToLong(new byte[]{0, 1, 1, 1}, 0, 2), 256);
        data = BytesUtils.toHighBytes(new byte[4], 1045, 1, 2);

        Assert.assertEquals(BytesUtils.highBytesToLong(data, 1, 2), 1045);

    }
}