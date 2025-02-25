package org.jetlinks.core.utils;

import org.junit.Test;
import org.springframework.core.ResolvableType;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ConverterUtilsTest {


    @Test
    public void testArray(){

       assertEquals(
            Arrays.asList("1","2"),
            ConverterUtils.convert(new String[]{"1","2"}, List.class)
        );

        assertEquals(
            Arrays.asList(1,2),
            ConverterUtils.convert(new String[]{"1","2"}, ResolvableType.forClassWithGenerics(List.class,Integer.class).getType())
        );

        assertEquals(
            Arrays.asList(1,2),
            ConverterUtils.convert(new long[]{1,2}, ResolvableType.forClassWithGenerics(List.class,Integer.class).getType())
        );
    }
}