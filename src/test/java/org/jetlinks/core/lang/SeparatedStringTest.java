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
    public void testAppendMulti() {
        String str = "test/1/2";
        SeparatedStringN string = SeparatedStringN.of('/', str.split("/"));

        SeparatedCharSequence c = string.append("3","4","5","/6/7");
        assertEquals("test/1/2/3/4/5/6/7", c.toString());

        assertEquals(8,c.size());

        assertEquals("1",c.get(1).toString());
        assertEquals("5",c.get(5).toString());
        assertEquals("6",c.get(6).toString());
        assertEquals("7",c.get(7).toString());


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
    public void testAppendCompatibility() {
        SharedPathString source = SharedPathString.of("/device/product/device", false);

        String segment1 = new String("temperature_alarm");
        String segment2 = new String("temperature_alarm");
        SeparatedCharSequence appended1 = source.append(segment1);
        SeparatedCharSequence appended2 = source.append(segment2);

        assertEquals(AppendSeparatedCharSequence.class, appended1.getClass());
        assertEquals("/device/product/device/temperature_alarm", appended1.toString());
        assertEquals(5, appended1.size());
        // 单段 append 现有语义会共享内容相同的 segment 引用。
        assertSame(appended1.get(4), appended2.get(4));
        assertEquals(appended1, appended2);
        assertEquals(appended1.hashCode(), appended2.hashCode());
        assertEquals(0, appended1.compareTo(appended2));

        assertAppendResult(source, "", "/device/product/device/", 5,
                           AppendSeparatedCharSequence.class);
        assertAppendResult(source, "/", "/device/product/device/", 5,
                           AppendSeparatedCharSequence.class);
        assertAppendResult(source, "/message/event", "/device/product/device/message/event", 6,
                           AppendSeparatedCharSequenceX.class);
        assertAppendResult(source, "message/event", "/device/product/device/message/event", 6,
                           AppendSeparatedCharSequenceX.class);
        assertAppendResult(source, "message/event/", "/device/product/device/message/event", 6,
                           AppendSeparatedCharSequenceX.class);
        assertAppendResult(source, "/message/event/", "/device/product/device/message/event", 6,
                           AppendSeparatedCharSequenceX.class);

        SeparatedCharSequence customSeparator = SeparatedString.create('|', "left", "right");
        assertAppendResult(customSeparator, "dynamic", "left|right|dynamic", 3,
                           AppendSeparatedCharSequence.class);
        assertAppendResult(customSeparator, "dynamic|next", "left|right|dynamic|next", 4,
                           AppendSeparatedCharSequenceX.class);

        StringBuilder mutable = new StringBuilder("event_before_mutation");
        SeparatedCharSequence snapshot = source.append(mutable);
        mutable.setLength(0);
        mutable.append("event_after_mutation");
        assertEquals("/device/product/device/event_before_mutation", snapshot.toString());

        SeparatedCharSequence eventTopic = source
            .append(SharedPathString.of("/message/event", false))
            .append("temperature_alarm");
        String expectedEventTopic = "/device/product/device/message/event/temperature_alarm";
        assertEquals(expectedEventTopic, eventTopic.toString());
        assertEquals(expectedEventTopic.length(), eventTopic.length());
        assertTrue(eventTopic.contentEquals(expectedEventTopic));
        assertFalse(eventTopic.contentEquals(
            "/device/product/device/message/event/temperature_alarx"));
        assertEquals(calculateLegacyHash(eventTopic), eventTopic.hashCode());

        SeparatedCharSequence deep = SharedPathString.of("/device", false);
        for (int i = 0; i < 8; i++) {
            deep = deep.append("temperature_alarm");
        }
        String expectedDeep = "/device"
            + "/temperature_alarm".repeat(8);
        assertEquals(AppendSeparatedCharSequence.class, deep.getClass());
        assertEquals(10, deep.size());
        assertEquals(expectedDeep, deep.toString());
        assertEquals(expectedDeep.length(), deep.length());
        assertTrue(deep.contentEquals(expectedDeep));
        assertEquals(calculateLegacyHash(deep), deep.hashCode());
    }

    private static int calculateLegacyHash(SeparatedCharSequence sequence) {
        int hash = sequence.getClass().hashCode();
        for (int i = 0, size = sequence.size(); i < size; i++) {
            hash = 31 * hash + sequence.get(i).hashCode() + sequence.separator();
        }
        return hash;
    }

    private static void assertAppendResult(SeparatedCharSequence source,
                                           String append,
                                           String expected,
                                           int expectedSize,
                                           Class<?> expectedType) {
        SeparatedCharSequence actual = source.append(append);
        assertEquals(expectedType, actual.getClass());
        assertEquals(expected, actual.toString());
        assertEquals(expectedSize, actual.size());
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
