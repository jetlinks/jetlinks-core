package org.jetlinks.core.utils;

import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.util.TypeUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.HeaderKey;
import org.jetlinks.core.utils.json.ObjectMappers;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;

public class ConverterUtils {


    @SuppressWarnings("all")
    public static <T> T convert(Object value, HeaderKey<T> key) {
        return convert(value, key.getValueType());
    }


    static Object getNullValue(Type type) {
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == char.class) {
            return '\u0000';
        }
        if (type == boolean.class) {
            return false;
        }
        return null;
    }

    @SuppressWarnings("all")
    public static <T> T convert(Object value, Type type) {
        if (value == null) {
            return (T) getNullValue(type);
        }

        if (type == Object.class ||
            (type instanceof Class && ((Class) type).isInstance(value))) {
            return (T) value;
        }

        if (type instanceof Class) {
            return (T) FastBeanCopier.DEFAULT_CONVERT.convert(
                value, (Class) type, FastBeanCopier.EMPTY_CLASS_ARRAY
            );
        }

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = ((ParameterizedType) type);
            Type rawType = parameterizedType.getRawType();

            if (rawType instanceof Class) {
                Type[] args = parameterizedType.getActualTypeArguments();

                Class[] arg = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Class) {
                        arg[i] = (Class) args[i];
                    } else {
                        arg[i] = ResolvableType.forType(args[i]).toClass();
                    }
                }

                return (T) FastBeanCopier.DEFAULT_CONVERT.convert(value, (Class) rawType, arg);
            }
        }

        return TypeUtils.cast(value, type, ParserConfig.getGlobalInstance());
    }

    /**
     * 尝试转换值为集合,如果不是集合格式则直接返回该值
     *
     * @param value     值
     * @param converter 转换器,用户转换单个结果
     * @return 转换结果
     */
    public static Object tryConvertToList(Object value, Function<Object, Object> converter) {
        List<Object> list = convertToList(value, converter);
        if (list.size() == 1) {
            return converter.apply(list.get(0));
        }
        return list;
    }

    /**
     * 转换参数为指定类型的List
     *
     * @param value     参数
     * @param converter 类型转换器
     * @param <T>       List中元素类型
     * @return 转换后的List
     */
    public static <T> List<T> convertToList(Object value, Function<Object, T> converter) {
        if (value == null) {
            return Collections.emptyList();
        }

        if (value instanceof String) {
            String[] arr = ((String) value).split(",");
            if (arr.length == 1) {
                return Collections.singletonList(converter.apply(arr[0]));
            }
            List<T> list = new ArrayList<>(arr.length);
            for (String s : arr) {
                list.add(converter.apply(s));
            }
            return list;
        }

        if (value instanceof Collection) {
            List<T> list = new ArrayList<>(((Collection<?>) value).size());
            for (Object o : ((Collection<?>) value)) {
                list.add(converter.apply(o));
            }
            return list;
        }

        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            List<T> list = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                list.add(converter.apply(Array.get(value, i)));
            }
            return list;
        }

        return Collections.singletonList(converter.apply(value));
    }

    /**
     * 转换参数为List
     *
     * @param value 参数
     * @return 排序后的流
     */
    public static List<Object> convertToList(Object value) {
        return convertToList(value, Function.identity());
    }


    /**
     * 将Map转为tag,如果map中到值不是数字,则转为json.
     * <pre>
     *      {"key1":"value1","key2":["value2"]} => key,value1,key2,["value2"]
     *  </pre>
     *
     * @param map map
     * @return tags
     */
    @SneakyThrows
    public static String[] convertMapToTags(Map<String, Object> map) {
        if (MapUtils.isEmpty(map)) {
            return new String[0];
        }
        String[] tags = new String[map.size() * 2];
        int index = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            String strValue = value instanceof String
                ? String.valueOf(value)
                : ObjectMappers.JSON_MAPPER.writeValueAsString(value);

            tags[index++] = key;
            tags[index++] = strValue;
        }
        if (tags.length > index) {
            return Arrays.copyOf(tags, index);
        }
        return tags;
    }

    private static final NettyDataBufferFactory factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

    public static DataBuffer convertDataBuffer(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof DataBuffer) {
            return ((DataBuffer) obj);
        }
        return factory.wrap(convertNettyBuffer(obj));
    }

    public static <T> ByteBuf convertNettyBuffer(T obj,
                                                 Function<T, ByteBuf> fallback) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof ByteBuf) {
            return ((ByteBuf) obj);
        }

        if (obj instanceof byte[]) {
            return Unpooled.wrappedBuffer(((byte[]) obj));
        }

        if (obj instanceof NettyDataBuffer) {
            return ((NettyDataBuffer) obj).getNativeBuffer();
        }

        if (obj instanceof DataBuffer) {
            return Unpooled.wrappedBuffer(((DataBuffer) obj).asByteBuffer());
        }

        if (obj instanceof ByteBuffer) {
            return Unpooled.wrappedBuffer(((ByteBuffer) obj));
        }

        if (obj instanceof String) {
            String str = String.valueOf(obj);
            // hex
            if (str.startsWith("0x")) {
                return Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(str, 2, str.length() - 2));
            }
            //data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA
            if (str.startsWith("data:")) {
                return Unpooled.wrappedBuffer(
                    Base64
                        .getDecoder()
                        .decode(str.substring(str.indexOf(",") + 1)));
            }
            // base64
            byte[] strBytes = str.getBytes();
            if (org.apache.commons.codec.binary.Base64.isBase64(strBytes)) {
                try {
                    return Unpooled.wrappedBuffer(
                        Base64
                            .getDecoder()
                            .decode(strBytes));
                } catch (Throwable ignore) {
                }
            }
            return Unpooled.wrappedBuffer(strBytes);
        }

        return fallback.apply(obj);
    }

    public static ByteBuf convertNettyBuffer(Object obj) {
        return convertNettyBuffer(obj, val -> Unpooled.wrappedBuffer(String.valueOf(val).getBytes()));
    }


    @SuppressWarnings("all")
    public static HttpHeaders convertHttpHeaders(Object headers) {
        if (headers instanceof HttpHeaders) {
            return (HttpHeaders) headers;
        }
        if (headers instanceof MultiValueMap) {
            return new HttpHeaders((MultiValueMap) headers);
        }
        if (headers instanceof Map<?, ?>) {
            Map<?, ?> httpHeaders = (Map<?, ?>) headers;
            HttpHeaders newHeader = new HttpHeaders();
            for (Map.Entry<?, ?> entry : httpHeaders.entrySet()) {
                newHeader.put(String.valueOf(entry.getKey()),
                              convertToList(entry.getValue(), String::valueOf));
            }
            return newHeader;
        }
        return ConverterUtils.convert(headers,HttpHeaders.class);
    }

}
