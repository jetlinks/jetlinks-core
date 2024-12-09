package org.jetlinks.core.lang;

import org.junit.Test;

import static org.junit.Assert.*;

public class SharedSeparatedStringTest {


    @Test
    public void test() {
        String str = "/test/1/2/3";

        SharedSeparatedString string = SharedSeparatedString.of('/', str);
        assertEquals(str, string.toString());
        assertEquals(str.length(), string.length());
        assertEquals(str.charAt(0), string.charAt(0));
        assertEquals(str.substring(1, 2), string.subSequence(1, 2).toString());

        System.out.println(string.hashCode());
    }

    @Test
    public void testHashCode() {
        SharedSeparatedString a = SharedSeparatedString.of('/', "/test/1/2/3");

        SharedSeparatedString b = SharedSeparatedString.of('/', "/test/1/2/3");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        SharedSeparatedString c = SharedSeparatedString.of('a', "/test/1/2/3".split("/"));
        assertNotEquals(a, c);
        assertNotEquals(a.hashCode(), c.hashCode());
    }
}