package org.jetlinks.core.lang;

import org.junit.Test;

import static org.junit.Assert.*;

public class SeparatedStringTest {


    @Test
    public void testRange() {
        String str = "/test/1/2/3";
        SharedPathString string = SharedPathString.of(str);

        assertEquals("test/1", string.range(1, 2).toString());
        assertEquals("/test/1", string.range(0, 3).toString());

    }

    @Test
    public void test() {
        String str = "/test/1/2/3";

        CharSequence string = SeparatedString.of('/', str);
        assertEquals(str, string.toString());
        assertEquals(str.length(), string.length());
        assertEquals(str.charAt(0), string.charAt(0));
        assertEquals(str.substring(1, 2), string.subSequence(1, 2).toString());

        System.out.println(string.hashCode());
    }


    @Test
    public void test2() {
        String str = "test/1";

        CharSequence string = SeparatedStringN.of('/', str);
        assertEquals(str, string.toString());
        assertEquals(str.length(), string.length());

        for (int i = 0; i < str.length(); i++) {
            assertEquals(str.charAt(i), string.charAt(i));
        }

        assertEquals(str.substring(1, 2), string.subSequence(1, 2).toString());

        System.out.println(string.hashCode());

    }

    @Test
    public void testAppend() {
        String str = "test/1/2";
        SeparatedStringN string = SeparatedStringN.of('/', str.split("/"));


        assertEquals("test/1/2/test", string.append("test").toString());

        assertEquals(string.append("test"), string.append("test"));
        assertEquals(string.append("test").hashCode(), string.append("test").hashCode());

        assertEquals(string.append("test/1"), string.append("test/1"));
        assertEquals(string.append("test/1").hashCode(), string.append("test/1").hashCode());

        assertEquals(string.append("test/1/2"), string.append("test/1/2"));
        assertEquals(0, string.append("test/1/2").compareTo(string.append("test/1/2")));

        assertEquals(string.append("test/1/2").hashCode(), string.append("test/1/2").hashCode());


        assertEquals("test/1/2/", string.append("/").toString());
        assertEquals("test/1/2/", string.append('/').toString());

        assertEquals("test/1/2/test/t2", string.append("test", "t2").toString());
        assertEquals("test/1/2/test/t2", string.append("test").append("t2").toString());

        assertEquals("test/1/2/test/t2/t3", string.append("test", "t2", "t3").toString());
        assertEquals("test/1/2/test/t2/t3/t4", string.append("test", "t2", "t3", "t4").toString());

        assertEquals(str + "/" + str, string.append(SeparatedStringN.of('/', str)).toString());

        assertEquals(str + "/" + str + "/test", string.append(SeparatedStringN.of('/', str), "test").toString());
    }

    @Test
    public void test3() {
        String str = "test/1/2";

        CharSequence string = SeparatedString.of('/', str);
        assertEquals(str, string.toString());
        assertEquals(str.length(), string.length());

        for (int i = 0; i < str.length(); i++) {
            assertEquals(str.charAt(i), string.charAt(i));
        }

        assertEquals(str.substring(1, 2), string.subSequence(1, 2).toString());

        System.out.println(string.hashCode());

    }

    @Test
    public void testHashCode() {
        CharSequence a = SeparatedString.of('/', "/test/1/2/3");

        CharSequence b = SeparatedString.of('/', "/test/1/2/3");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        CharSequence c = SeparatedStringN.of('a', "/test/1/2/3".split("/"));
        assertNotEquals(a, c);
        assertNotEquals(a.hashCode(), c.hashCode());
    }
}