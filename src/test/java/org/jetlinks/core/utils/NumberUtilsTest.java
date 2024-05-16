package org.jetlinks.core.utils;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class NumberUtilsTest {

    @Test
   public void testNumberOfPlace(){
        assertEquals(2,NumberUtils.numberOfPlace(1.23));
        assertEquals(2,NumberUtils.numberOfPlace(10.23));
        assertEquals(3,NumberUtils.numberOfPlace(-10.003));

        assertEquals(7,NumberUtils.numberOfPlace(1.0000001D));

        assertEquals(0,NumberUtils.numberOfPlace(1));
        assertEquals(0,NumberUtils.numberOfPlace(1.0));

        assertEquals(0,NumberUtils.numberOfPlace(new BigDecimal("1.0")));
    }

    @Test
    public void testScale(){
       assertEquals(
           1.232,
           NumberUtils.convertEffectiveScale(1.23221, 3),
           0
       );

        assertEquals(
            0.00322,
            NumberUtils.convertEffectiveScale(0.003216, 3),
            0
        );

        assertEquals(
            2.0,
            NumberUtils.convertEffectiveScale(2.0, 3),
            0
        );
    }
}