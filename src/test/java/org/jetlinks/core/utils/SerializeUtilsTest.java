package org.jetlinks.core.utils;

import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import lombok.*;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.authorization.token.TokenState;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.I18nSupportException;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.proxy.Proxy;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.exception.RecursiveCallException;
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
    @SneakyThrows
    public void testImmutableList() {
        Object obj = codec(List.of(1, 2, 3));
        System.out.println(obj);
        assertNotNull(obj);
        assertTrue(obj instanceof List<?>);
        assertEquals(3, ((List<?>) obj).size());
    }


    @Test
    @SneakyThrows
    public void testImmutableMap() {

        Map<Object, Object> v = Map.of(1, 11, 2, 22);
        Object obj = codec(v);
        System.out.println(obj);
        assertNotNull(obj);
        assertTrue(obj instanceof Map<?, ?>);
        assertEquals(v, obj);
    }

    @Test
    public void testDeepTransferCollection() {

        {
            Object val = codec(Maps.filterEntries(new HashMap<>(), Objects::nonNull).values());

            assertTrue(val instanceof Collection);

        }
        {
            Object val = codec(Maps.filterEntries(new HashMap<>(), Objects::nonNull).keySet());

            assertTrue(val instanceof Set);
        }

        {
            Object val = codec(Collections2.filter(Maps
                                                       .filterEntries(new HashMap<>(), Objects::nonNull)
                                                       .keySet(), Objects::nonNull));

            assertTrue(val instanceof Collection);
        }
    }

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

    public static class CustomMap extends HashMap<String, Object> implements Externalizable {
        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            SerializeUtils.writeKeyValue(this, out);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            SerializeUtils.readKeyValue(in, this::put);
        }
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

    @Test
    public void testExceptionSerialization() {
        // 测试普通异常
        RuntimeException runtimeException = new RuntimeException("测试异常");
        Throwable decoded = (Throwable) codec(runtimeException);
        assertNotNull(decoded);
        assertEquals("测试异常", decoded.getMessage());
        assertTrue(decoded instanceof BusinessException.NoStackTrace);

        // 测试 BusinessException
        BusinessException businessException = new BusinessException.NoStackTrace("业务异常", 400, "error.business", "arg1", "arg2");
        decoded = (Throwable) codec(businessException);
        assertNotNull(decoded);
        assertTrue(decoded instanceof BusinessException.NoStackTrace);
        BusinessException.NoStackTrace decodedBusiness = (BusinessException.NoStackTrace) decoded;
        assertEquals(400, decodedBusiness.getStatus());
        assertEquals("业务异常", decodedBusiness.getMessage());
        // 注意：getI18nCode() 可能返回本地化后的消息，这里只验证消息和状态码
        if (decodedBusiness instanceof I18nSupportException) {
            I18nSupportException i18nException = (I18nSupportException) decodedBusiness;
            // 验证 i18n 代码或消息不为空
            assertNotNull(i18nException.getI18nCode());
        }
        assertNotNull(decodedBusiness.getArgs());
        assertEquals(2, decodedBusiness.getArgs().length);

        // 测试 NotFoundException
        NotFoundException notFoundException = new NotFoundException.NoStackTrace("未找到", "arg1");
        decoded = (Throwable) codec(notFoundException);
        assertNotNull(decoded);
        assertTrue(decoded instanceof NotFoundException.NoStackTrace);
        assertEquals("未找到", decoded.getMessage());

        // 测试 ValidationException
        List<ValidationException.Detail> details = new ArrayList<>();
        details.add(new ValidationException.Detail("field1", "错误1", "detail1"));
        details.add(new ValidationException.Detail("field2", "错误2", "detail2"));
        ValidationException validationException = new ValidationException.NoStackTrace("验证失败", details, "arg1");
        decoded = (Throwable) codec(validationException);
        assertNotNull(decoded);
        assertTrue(decoded instanceof ValidationException.NoStackTrace);
        ValidationException.NoStackTrace decodedValidation = (ValidationException.NoStackTrace) decoded;
        assertEquals("验证失败", decodedValidation.getMessage());
        List<ValidationException.Detail> decodedDetails = decodedValidation.getDetails();
        assertNotNull(decodedDetails);
        assertEquals(2, decodedDetails.size());
        assertEquals("field1", decodedDetails.get(0).getProperty());
        assertEquals("错误1", decodedDetails.get(0).getMessage());

        // 测试 UnAuthorizedException
        UnAuthorizedException unAuthorizedException = new UnAuthorizedException.NoStackTrace("未授权", TokenState.expired);
        decoded = (Throwable) codec(unAuthorizedException);
        assertNotNull(decoded);
        assertTrue(decoded instanceof UnAuthorizedException.NoStackTrace);
        UnAuthorizedException.NoStackTrace decodedUnauthorized = (UnAuthorizedException.NoStackTrace) decoded;
        assertEquals("未授权", decodedUnauthorized.getMessage());
        assertEquals(TokenState.expired, decodedUnauthorized.getState());

        // 测试 AccessDenyException
        AccessDenyException accessDenyException = new AccessDenyException.NoStackTrace("拒绝访问");
        decoded = (Throwable) codec(accessDenyException);
        assertNotNull(decoded);
        assertTrue(decoded instanceof AccessDenyException.NoStackTrace);
        assertEquals("拒绝访问", decoded.getMessage());

        // 测试 DeviceOperationException
        DeviceOperationException deviceException = new DeviceOperationException.NoStackTrace(ErrorCode.TIME_OUT, "设备操作超时");
        decoded = (Throwable) codec(deviceException);
        assertNotNull(decoded);
        assertTrue(decoded instanceof DeviceOperationException.NoStackTrace);
        DeviceOperationException.NoStackTrace decodedDevice = (DeviceOperationException.NoStackTrace) decoded;
        assertEquals(ErrorCode.TIME_OUT, decodedDevice.getCode());
        assertEquals("设备操作超时", decodedDevice.getMessage());

        // 测试 RecursiveCallException
        RecursiveCallException recursiveException = new RecursiveCallException("testOperation", 5);
        decoded = (Throwable) codec(recursiveException);
        assertNotNull(decoded);
        assertTrue(decoded instanceof RecursiveCallException);
        RecursiveCallException decodedRecursive = (RecursiveCallException) decoded;
        assertEquals("testOperation", decodedRecursive.getOperation());
        assertEquals(5, decodedRecursive.getMaxRecursive());

        // 测试 I18nSupportException
        I18nSupportException i18nException = new I18nSupportException.NoStackTrace("i18n错误", "arg1", "arg2");
        decoded = (Throwable) codec(i18nException);
        assertNotNull(decoded);
        assertTrue(decoded instanceof I18nSupportException.NoStackTrace);
        I18nSupportException.NoStackTrace decodedI18n = (I18nSupportException.NoStackTrace) decoded;
        assertEquals("i18n错误", decodedI18n.getMessage());
        // 注意：getI18nCode() 可能返回本地化后的消息，这里只验证 i18n 代码不为空
        assertNotNull(decodedI18n.getI18nCode());
        assertNotNull(decodedI18n.getArgs());
        assertEquals(2, decodedI18n.getArgs().length);

        // 测试带堆栈跟踪的异常
        RuntimeException exceptionWithStackTrace = new RuntimeException("带堆栈的异常");
        exceptionWithStackTrace.fillInStackTrace();
        decoded = (Throwable) codec(exceptionWithStackTrace);
        assertNotNull(decoded);
        assertNotNull(decoded.getStackTrace());
        assertTrue(decoded.getStackTrace().length > 0);
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
            Object decode = SerializeUtils.readObject(obIn);
            System.out.printf("codec from %s to %s \n", obj == null ? "null" : obj.getClass(), decode == null ? "null" : decode.getClass());
            return decode;
        }
    }
}