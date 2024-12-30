package org.jetlinks.core.lang;

import lombok.SneakyThrows;
import org.jetlinks.core.utils.SerializeUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.*;

public class SeparatedStringTest {


    @Test
    public void testContentEquals(){
        String str = "/1/2/3/4/5";
        SharedPathString string = SharedPathString.of(str);

        assertTrue(string.contentEquals("/1/2/3/4/5"));

        assertTrue(string.contentEquals(SeparatedString.create('/', "","1", "2", "3", "4", "5")));

    }

    @Test
    public void testReplace() {
        String str = "/1/2/3/4/5";
        SharedPathString string = SharedPathString.of(str);

        assertEquals("/2/2/3/4/5", string.replace(1, "2").toString());

        assertEquals("/1/2/4/5/5", string.replace(3, "4", 4, "5").toString());

        assertEquals("/1/2/1/5/5", string.replace(3, "4", 4, "5").replace(3, "1").toString());

        assertEquals("/1/2/4/5/6", string.replace(3, "4", 4, "5", 5, "6").toString());


    }

    @SneakyThrows
    public Object codec(Object obj) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ObjectOutputStream objOut = new ObjectOutputStream(output)) {
            SerializeUtils.writeObject(obj, objOut);
        }
        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        try (ObjectInputStream obIn = new ObjectInputStream(input)) {
            return SerializeUtils.readObject(obIn);
        }
    }

    @Test
    public void testSer() {
        String str = "/test/1/2/3";
        SharedPathString string = SharedPathString.of(str);

        assertEquals(string, codec(string));
    }

    @Test
    public void testRange() {
        String str = "/test/1/2/3";
        SharedPathString string = SharedPathString.of(str);

        assertEquals("test/1", string.range(1, 3).toString());
        assertEquals("/test/1", string.range(0, 3).toString());

        assertEquals(string.range(0, 3).toString(),
                     codec(string.range(0, 3)).toString());
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

        assertEquals("test/1/2/test", string.append(SharedPathString.of("/test")).toString());

        assertEquals("test/1/2/test/test2",
                     string.append("test",
                                   SharedPathString.of("/test2"))
                           .toString());


        assertEquals(string.append("test"), string.append("test"));
        assertEquals(string.append("test").hashCode(), string.append("test").hashCode());

        assertEquals(string.append("test/1"), string.append("test/1"));
        assertEquals(string.append("test/1").hashCode(), string.append("test/1").hashCode());

        assertEquals(string.append("test/1/2"), string.append("test/1/2"));

        assertEquals(0, string.append("test/1/2").compareTo(string.append("test/1/2")));

        assertEquals(string.append("test/1/2").hashCode(), string.append("test/1/2").hashCode());


        assertEquals("test/1/2/", string.append("").toString());
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