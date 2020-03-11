package org.jetlinks.core.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class BytesUtilsTest {

    @Test
    public void testInt() {
        Assert.assertEquals(BytesUtils.leToInt(BytesUtils.intToLe(32)), 32);
        Assert.assertEquals(BytesUtils.leToInt(BytesUtils.intToLe(32)), 32);

        Assert.assertEquals(BytesUtils.leToInt(BytesUtils.intToLe(Short.MIN_VALUE)), Short.MIN_VALUE);
        Assert.assertEquals(BytesUtils.leToInt(BytesUtils.intToLe(Integer.MIN_VALUE)), Integer.MIN_VALUE);
        Assert.assertEquals(BytesUtils.leToInt(BytesUtils.intToLe(Integer.MAX_VALUE)), Integer.MAX_VALUE);

    }

    @Test
    public void testLong() {
        {
            long val = new Random().nextLong();

            Assert.assertEquals(BytesUtils.leToLong(BytesUtils.longToLe(val)), val);
            Assert.assertEquals(BytesUtils.beToLong(BytesUtils.longToBe(val)), val);
        }

        {
            long val = -new Random().nextLong();

            Assert.assertEquals(BytesUtils.leToLong(BytesUtils.longToLe(val)), val);
            Assert.assertEquals(BytesUtils.beToLong(BytesUtils.longToBe(val)), val);
        }
    }

    @Test
    public void testDouble() {
        {
            double val = new Random().nextDouble();
            Assert.assertEquals(BytesUtils.leToDouble(BytesUtils.doubleToLe(val)), val, 0);
            Assert.assertEquals(BytesUtils.beToDouble(BytesUtils.doubleToBe(val)), val, 0);
        }
        {
            double val = -new Random().nextDouble();
            Assert.assertEquals(BytesUtils.leToDouble(BytesUtils.doubleToLe(val)), val, 0);
            Assert.assertEquals(BytesUtils.beToDouble(BytesUtils.doubleToBe(val)), val, 0);
        }
    }

    @Test
    public void testFloat() {
        {
            float val = new Random().nextFloat();
            Assert.assertEquals(BytesUtils.leToFloat(BytesUtils.floatToLe(val)), val, 0);
            Assert.assertEquals(BytesUtils.beToFloat(BytesUtils.floatToBe(val)), val, 0);
        }
        {
            float val = -new Random().nextFloat();
            Assert.assertEquals(BytesUtils.leToFloat(BytesUtils.floatToLe(val)), val, 0);
            Assert.assertEquals(BytesUtils.beToFloat(BytesUtils.floatToBe(val)), val, 0);
        }
    }

    @Test
    public void testOffset() {

        byte[] data = BytesUtils.numberToLe(new byte[4], 566, 2, 2);

        Assert.assertEquals(BytesUtils.leToInt(data, 2, 2), 566);

        Assert.assertEquals(BytesUtils.beToInt(new byte[]{2, 1, 0, 2}, 1, 2), 256);

        Assert.assertEquals(BytesUtils.beToInt(new byte[]{0, 1, 2, 2}, 0, 2), 1);

        Assert.assertEquals(BytesUtils.leToLong(new byte[]{0, 1, 1, 1}, 0, 2), 256);
        data = BytesUtils.numberToLe(new byte[4], 1045, 1, 2);

        Assert.assertEquals(BytesUtils.leToInt(data, 1, 2), 1045);

    }

    @Test
    public void testReverse() {
        {
            byte[] arr = new byte[]{1, 2, 3, 4};
            Assert.assertArrayEquals(BytesUtils.reverse(arr), new byte[]{4, 3, 2, 1});
        }

        {
            byte[] arr = new byte[]{1, 2, 3, 4, 5};
            Assert.assertArrayEquals(BytesUtils.reverse(arr, 1, 2), new byte[]{1, 3, 2, 4, 5});
        }

        {
            byte[] arr = new byte[]{1, 2, 3, 4, 5};
            Assert.assertArrayEquals(BytesUtils.reverse(arr, 1, 3), new byte[]{1, 4, 3, 2, 5});
        }

        {
            byte[] arr2 = new byte[]{1, 2, 3, 4, 5};
            Assert.assertArrayEquals(BytesUtils.reverse(arr2, 1, 4), new byte[]{1, 5, 4, 3, 2});
        }

        {
            byte[] arr2 = new byte[]{1, 2, 3, 4, 5};
            Assert.assertArrayEquals(BytesUtils.reverse(arr2, 2, 4), new byte[]{1, 2, 5, 4, 3});
        }


    }
}