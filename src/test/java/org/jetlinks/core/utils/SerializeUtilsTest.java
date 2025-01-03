package org.jetlinks.core.utils;

import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import lombok.*;
import org.hswebframework.web.proxy.Proxy;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.ThingMessage;
import org.junit.Test;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

public class SerializeUtilsTest {


    @Test
    public void testStackTraceElement() {

        Object trace = codec(new Exception().getStackTrace());

        assertTrue(trace instanceof StackTraceElement[]);
        StackTraceElement[] arr = (StackTraceElement[]) trace;

        Exception e = new Exception();
        e.setStackTrace(arr);


    }

    @Test
    public void testConvertToSafelySerializable() {
        assertEquals(1, SerializeUtils.convertToSafelySerializable(1));

        assertArrayEquals(new byte[]{0x01}, (byte[]) SerializeUtils
            .convertToSafelySerializable(Unpooled.buffer().writeByte(0x01)));

        assertArrayEquals(new byte[]{0x01}, (byte[]) SerializeUtils
            .convertToSafelySerializable(new byte[]{0x01}));

    }


    @Test
    public void testPrimitive() {
        assertEquals(Byte.MAX_VALUE, codec(Byte.MAX_VALUE));
        assertEquals(Byte.MIN_VALUE, codec(Byte.MIN_VALUE));

        assertEquals(Short.MAX_VALUE, codec(Short.MAX_VALUE));
        assertEquals(Short.MIN_VALUE, codec(Short.MIN_VALUE));

        assertEquals(Integer.MAX_VALUE, codec(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, codec(Integer.MIN_VALUE));

        assertEquals(Long.MAX_VALUE, codec(Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, codec(Long.MIN_VALUE));

        assertEquals(Float.MAX_VALUE, codec(Float.MAX_VALUE));
        assertEquals(Float.MIN_VALUE, codec(Float.MIN_VALUE));

        assertEquals(Double.MAX_VALUE, codec(Double.MAX_VALUE));
        assertEquals(Double.MIN_VALUE, codec(Double.MIN_VALUE));

    }

    @Test
    public void testString() {

        assertEquals("test", codec("test"));
        assertEquals("中文", codec("中文"));

        assertEquals("\r\n", codec("\r\n"));


    }

    @Test
    public void testArray() {

        assertArrayEquals(new int[]{1, 2, 3}, (int[]) codec(new int[]{1, 2, 3}));

        assertArrayEquals(new Integer[]{1, 2, 3}, (Integer[]) codec(new Integer[]{1, 2, 3}));


    }


    @Test
    public void testList() {

        assertEquals(Arrays.asList(1, 2, 3), codec(Arrays.asList(1, 2, 3)));

        assertEquals(Arrays.asList(1, "2", 3), codec(Arrays.asList(1, "2", 3)));


    }

    @Test
    public void testSet() {

        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), codec(new HashSet<>(Arrays.asList(1, 2, 3))));

        assertEquals(new HashSet<>(Arrays.asList(1, "2", 3)), codec(new HashSet<>(Arrays.asList(1, "2", 3))));

        Set<Object> set = ConcurrentHashMap.newKeySet();

        set.addAll(Arrays.asList(1, "2", 3));

        assertEquals(ConcurrentHashMap.KeySetView.class, codec(set).getClass());

    }

    @Test
    public void testBigDecimal() {
        BigDecimal bigDecimal = new BigDecimal("12341315613123123789.12341231212312313312390123847289634591827634581723456789");
        assertEquals(bigDecimal, codec(bigDecimal));

        assertEquals(BigDecimal.ONE, codec(BigDecimal.ONE));
        assertEquals(BigDecimal.ZERO, codec(BigDecimal.ZERO));
        assertEquals(BigDecimal.valueOf(1.23), codec(BigDecimal.valueOf(1.23)));

    }

    @Test
    public void testBigInteger() {
        BigInteger integer = new BigInteger("123413156131231237891231924872019368451289734561289375641247856128745678234568");
        assertEquals(integer, codec(integer));

        assertEquals(BigInteger.ONE, codec(BigInteger.ONE));
        assertEquals(BigInteger.ZERO, codec(BigInteger.ZERO));
        assertEquals(BigInteger.valueOf(123123), codec(BigInteger.valueOf(123123)));

    }


    @Test
    public void testMap() {

        assertEquals(Collections.singletonMap("test", Arrays.asList(1, 2, 3)), codec(Collections.singletonMap("test", Arrays.asList(1, 2, 3))));


        assertEquals(Collections.singletonMap("test", null), codec(Collections.singletonMap("test", null)));

        assertEquals(ConcurrentHashMap.class, codec(new ConcurrentHashMap<>(Collections.singletonMap("test", Arrays.asList(1, 2, 3)))).getClass());

    }

    @Test
    public void testJson() {

        assertEquals(new JsonData("test"), codec(new JsonData("test")));

    }

    @Test
    public void testEnum() {

        for (EnumTest value : EnumTest.values()) {
            assertEquals(value, codec(value));

        }

    }

    @Test
    public void testMessage() {
        for (MessageType value : MessageType.values()) {
            {
                DeviceMessage msg = value.forDevice();
                if (msg != null) {
                    DeviceMessage decode = (DeviceMessage) codec(msg);

                    assertEquals(msg.getTimestamp(), decode.getTimestamp());
                }
            }
            {
                ThingMessage msg = value.forThing("test", "test1");
                if (msg != null) {
                    ThingMessage decode = (ThingMessage) codec(msg);
                    assertEquals(msg.getThingType(), decode.getThingType());
                    assertEquals(msg.getTimestamp(), decode.getTimestamp());
                }
            }
        }
    }

    @Test
    public void testGuavaMap() {
        Object res = codec(Maps.filterEntries(Collections.emptyMap(), entry -> true));
        assertNotNull(res);
        assertTrue(res instanceof Map);
    }


    @Test
    public void testEmptyMap() {
        Object res = codec(Collections.emptyMap());
        assertNotNull(res);
        assertTrue(res instanceof Map);
    }

    @Test
    public void testCustomMap() {
        Object res = codec(new CustomMap());
        assertNotNull(res);
        assertTrue(res instanceof CustomMap);
    }

    @Test
    public void testCrossClassLoader() {

        Object obj = Proxy.create(Serializable.class)
                          .addField("private int code;")
                          .addMethod("public int getCode(){return code;}")
                          .addMethod("public void setCode(int code){this.code = code;}")
                          .newInstance();
        assertNotNull(obj);
        System.out.println(obj);
        Object res = codec(obj);
        assertNotNull(res);
        assertTrue(res instanceof Map);
        assertEquals(0, ((Map<?, ?>) res).get("code"));

        //to map
        assertNotNull(SerializeUtils.convertToSafelySerializable(obj));

        //array
        Object arr = codec(new Object[]{obj, obj, obj});
        assertNotNull(arr);

        //list
        Object list = codec(Arrays.asList(obj, obj, obj));
        assertTrue(list instanceof List);
        assertEquals(3, ((List<?>) list).size());

        //map
        Object map = codec(Collections.singletonMap("test", obj));
        assertNotNull(map);

        //deepMap
        Object deepMap = codec(Collections.singletonMap("1", Collections.singletonMap("2", obj)));
        assertNotNull(deepMap);
        System.out.println(deepMap);
    }

    public static class CustomMap extends HashMap<String, Object> {
    }


    @Test
    public void testByteBuf() {

        ByteBuf buf = ByteBufAllocator.DEFAULT
            .buffer()
            .writeInt(100);

        ByteBuf decode = (ByteBuf) codec(buf);

        assertEquals(100, decode.readInt());

    }

    @Test
    public void testNio() {

        ByteBuffer buf = ByteBuffer.allocate(32);
        buf.putInt(100);

        ByteBuffer decode = (ByteBuffer) codec(buf);

        assertEquals(100, decode.getInt());

    }

    public enum EnumTest {
        A, B, C
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @EqualsAndHashCode(of = "data")
    public static class JsonData {
        private String data;
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
}