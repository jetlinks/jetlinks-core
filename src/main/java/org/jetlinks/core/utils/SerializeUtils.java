package org.jetlinks.core.utils;

import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SeparatedString;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.metadata.Jsonable;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SerializeUtils {
    static final Serializer[] all = new Serializer[256];
    private static final Map<Class<?>, Serializer> cache = new ConcurrentHashMap<>();
    private static final Map<String, Class<?>> clazzCache = new ConcurrentReferenceHashMap<>();

    static {
        for (InternalSerializers value : InternalSerializers.values()) {
            registerSerializer(value);
        }
        registerSerializer(new TypedMapSerializer());
        registerSerializer(new TypedCollectionSerializer());

        cache.put(StackTraceElement.class, InternalSerializers.StackTraceElement);
    }

    private static final Set<Class<?>> safelySerializable = ConcurrentHashMap.newKeySet();

    static {
        addSafelySerializable(DateTime.class,
                              LocalDateTime.class,
                              LocalDate.class);
    }

    public static void addSafelySerializable(Class<?>... type) {
        safelySerializable.addAll(Arrays.asList(type));
    }

    /**
     * 将对象转换为可安全序列化的对象,如果对象是java bean将会转为Map.
     *
     * @param value 对象
     * @return 转换后的对象
     */
    public static Object convertToSafelySerializable(Object value) {

        return convertToSafelySerializable(value, false);

    }

    /**
     * 将对象转换为可安全序列化的对象,如果对象是java bean将会转为Map.
     *
     * @param value 对象
     * @param copy  是否赋值对象
     * @return 转换后的对象
     */
    public static Object convertToSafelySerializable(Object value, boolean copy) {
        if (value == null ||
            value instanceof CharSequence ||
            value instanceof Character ||
            value instanceof Number ||
            value instanceof Boolean ||
            value instanceof Date ||
            value instanceof TemporalAccessor) {
            return value;
        }

        if (value instanceof Jsonable) {
            Map<?, ?> m = Maps.transformValues(((Jsonable) value).toJson(), val -> convertToSafelySerializable(val, copy));
            return copy ? new HashMap<>(m) : m;
        }

        if (value instanceof Map) {
            Map<?, ?> m = Maps.transformValues(((Map<?, ?>) value), map -> convertToSafelySerializable(map, copy));
            return copy ? new HashMap<>(m) : m;
        }

        if (value instanceof Collection) {
            Collection<?> c = Collections2.transform(((Collection<?>) value), conn -> convertToSafelySerializable(conn, copy));
            if (!copy) {
                return c;
            }
            if (value instanceof Set) {
                return new HashSet<>(c);
            }
            return new ArrayList<>(c);
        }
        if (value instanceof EnumDict) {
            return ((EnumDict<?>) value).getWriteJSONObject();
        }
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }

        {
            if (value instanceof ByteBuffer) {
                value = Unpooled.wrappedBuffer(((ByteBuffer) value));
            }
            if (value instanceof ByteBuf) {
                try {
                    return ByteBufUtil.getBytes(((ByteBuf) value));
                } finally {
                    ReferenceCountUtil.safeRelease(value);
                }
            }
        }

        Class<?> clazz = value.getClass();

        if (clazz.isArray()) {
            Class<?> ctype = clazz.getComponentType();
            if (ctype.isPrimitive() || safelySerializable.contains(ctype)) {
                return value;
            }
            int len = Array.getLength(value);
            Object[] val = new Object[len];
            for (int i = 0; i < len; i++) {
                val[i] = convertToSafelySerializable(Array.get(value, i), copy);
            }
            return val;
        }

        if (clazz.getName().startsWith("java.")) {
            return true;
        }
        if (safelySerializable.contains(value.getClass())) {
            return value;
        }

        return convertToSafelySerializable(FastBeanCopier.copy(value, new LinkedHashMap<>()), copy);
    }

    public static synchronized void registerSerializer(Serializer serializer) {
        if (serializer.getCode() > 255) {
            throw new IllegalArgumentException("serializer code must be less than 255");
        }

        int code = serializer.getCode() & 0xFF;
        if (all[code] != null && !Objects.equals(all[code], serializer)) {
            throw new IllegalArgumentException("serializer code [" + serializer.getCode() + "] already exists,type:" + all[code].getJavaType());
        }

        all[code] = serializer;
        if (!(serializer instanceof InternalSerializers)) {
            cache.put(serializer.getJavaType(), serializer);
        }
    }

    public static <T extends Externalizable> void registerSerializer(int code,
                                                                     Class<T> type,
                                                                     Function<ObjectInput, ? extends T> newInstance) {
        registerSerializer(new ExternalSerializer(code, type, newInstance));
    }

    @SneakyThrows
    public static String readNullableUTF(ObjectInput in) {
        if (in.readBoolean()) {
            return null;
        }
        return in.readUTF();
    }

    @SneakyThrows
    public static void writeNullableUTF(String str, ObjectOutput out) {
        if (str == null) {
            out.writeBoolean(true);
            return;
        }
        out.writeBoolean(false);
        out.writeUTF(str);
    }

    @SneakyThrows
    public static void writeObject(Object obj, ObjectOutput out) {
        Serializer serializer;
        if (obj == null) {
            serializer = InternalSerializers.NULL;
        } else {
            serializer = lookup(obj);
        }
        out.writeByte(serializer.getCode());
        serializer.serialize(obj, out);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T> T readObjectAs(ObjectInput input) {
        return (T) readObject(input);
    }

    @SneakyThrows
    public static Object readObject(ObjectInput input) {
        Serializer serializer = all[input.readUnsignedByte()];
        if (serializer == null) {
            return null;
        }
        return serializer.deserialize(input);
    }

    public static Serializer lookup(Object readyToSer) {
        if (readyToSer instanceof String) {
            return InternalSerializers.STRING;
        }
        if (readyToSer instanceof Boolean) {
            return InternalSerializers.BOOLEAN;
        }
        if (readyToSer instanceof Integer) {
            return InternalSerializers.INT;
        }
        if (readyToSer instanceof Long) {
            return InternalSerializers.LONG;
        }
        if (readyToSer instanceof Double) {
            return InternalSerializers.DOUBLE;
        }
        if (readyToSer instanceof Float) {
            return InternalSerializers.FLOAT;
        }
        if (readyToSer instanceof Byte) {
            return InternalSerializers.BYTE;
        }
        if (readyToSer instanceof Short) {
            return InternalSerializers.SHORT;
        }
        if (readyToSer instanceof Message) {
            return InternalSerializers.MESSAGE;
        }
        if (readyToSer instanceof Character) {
            return InternalSerializers.CHAR;
        }
        if (readyToSer instanceof BigDecimal) {
            return InternalSerializers.BIG_DECIMAL;
        }
        if (readyToSer instanceof BigInteger) {
            return InternalSerializers.BIG_INTEGER;
        }
        if (readyToSer instanceof ConcurrentMap) {
            return InternalSerializers.C_MAP;
        }
        if (readyToSer instanceof ConcurrentHashMap.KeySetView) {
            return InternalSerializers.C_SET;
        }
        if (readyToSer instanceof Map) {
            return all[TypedMapSerializer.CODE];
        }
//        if (readyToSer instanceof Set) {
//            return InternalSerializers.SET;
//        }
        if (readyToSer instanceof Collection) {
            return all[TypedCollectionSerializer.CODE];
        }
        if (readyToSer instanceof ByteBuf) {
            return InternalSerializers.Netty;
        }
        if (readyToSer instanceof ByteBuffer) {
            return InternalSerializers.Nio;
        }
        if (readyToSer instanceof SharedPathString) {
            return InternalSerializers.SharedPathStringSer;
        }
        if (readyToSer instanceof SeparatedCharSequence) {
            return InternalSerializers.SeparatedCharSequenceSer;
        }
        return lookup(readyToSer.getClass());
    }

    public static Serializer lookup(Class<?> javaType) {
        return cache.computeIfAbsent(javaType, t -> {
            if (t.isPrimitive()) {
                t = Primitives.wrap(t);
            }
            //不是同一个classLoader.使用json序列化.
            if (t.getClassLoader() != null && t.getClassLoader() != ClassUtils.getDefaultClassLoader()) {
                return InternalSerializers.JSON;
            }

            for (Serializer type : all) {
                if (type == null) {
                    continue;
                }
                if (type.getJavaType() == t || type.getJavaType().isAssignableFrom(t)) {
                    return type;
                }
            }
            if (t.isArray() && !t.getComponentType().isPrimitive()) {
                return InternalSerializers.ARRAY;
            }
            return InternalSerializers.JSON;
        });
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <K, T> Map<K, T> readMap(ObjectInput in,
                                           Function<Integer, Map<K, T>> mapBuilder) {
        //header
        int headerSize = in.readInt();

        Map<K, T> map = mapBuilder.apply(Math.max(8, headerSize));

        for (int i = 0; i < headerSize; i++) {
            Object key = readObject(in);
            Object value = readObject(in);
            map.put((K) key, (T) value);
        }
        return map;
    }

    @SneakyThrows
    public static <K, T> Map<K, T> readMap(ObjectInput in,
                                           Function<Object, K> keyMapper,
                                           Function<Object, T> valueMapper,
                                           Function<Integer, Map<K, T>> mapBuilder) {
        //header
        int headerSize = in.readInt();

        Map<K, T> map = mapBuilder.apply(Math.max(8, headerSize));

        for (int i = 0; i < headerSize; i++) {
            K key = keyMapper.apply(readObject(in));
            T value = valueMapper.apply(readObject(in));
            map.put(key, value);
        }
        return map;
    }

    @SneakyThrows
    public static void readKeyValue(ObjectInput in,
                                    BiConsumer<String, Object> consumer) {
        //header
        int headerSize = in.readInt();
        for (int i = 0; i < headerSize; i++) {
            String key = String.valueOf(readObject(in));
            consumer.accept(key, readObject(in));
        }
    }

    @SneakyThrows
    public static void writeKeyValue(Map<?, ?> map, ObjectOutput out) {
        if (map == null) {
            out.writeInt(0);
        } else {
            out.writeInt(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                writeObject(entry.getKey(), out);
                writeObject(entry.getValue(), out);
            }
        }
    }

    @SneakyThrows
    public static <T> void writeKeyValue(Collection<T> collection,
                                         Function<T, Object> keyMapper,
                                         Function<T, Object> valueMapper,
                                         ObjectOutput out) {
        if (collection == null) {
            out.writeInt(0);
        } else {
            out.writeInt(collection.size());
            for (T t : collection) {
                Object key = keyMapper.apply(t);
                Object value = valueMapper.apply(t);
                writeObject(key, out);
                writeObject(value, out);
            }
        }
    }

    @SneakyThrows
    private static Class<?> loadClass(String name) {
        return SerializeUtils.class.getClassLoader().loadClass(name);
    }

    @SneakyThrows
    @SuppressWarnings("all")
    static <T> Class<T> getClass(String name) {
        return (Class<T>) clazzCache.computeIfAbsent(name, SerializeUtils::loadClass);
    }

    @Getter
    @AllArgsConstructor
    private enum InternalSerializers implements Serializer {
        NULL(0x00, Void.class) {
            @Override
            public Object read(ObjectInput in) {
                return null;
            }

            @Override
            void write(Object value, ObjectOutput input) {

            }
        },
        BOOLEAN(0x01, Boolean.class) {
            @Override
            @SneakyThrows
            public Object read(ObjectInput in) {
                return in.readBoolean();
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                input.writeBoolean((Boolean) value);
            }
        },
        BYTE(0x02, Byte.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                return input.readByte();
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                input.writeByte(((Byte) value));
            }
        },
        CHAR(0x03, Character.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                return input.readChar();
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                input.writeChar(((Character) value));
            }
        },
        SHORT(0x04, Short.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                return input.readShort();
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                input.writeShort(((Short) value));
            }
        },
        INT(0x05, Integer.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                return input.readInt();
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                input.writeInt(((Integer) value));
            }
        },
        LONG(0x06, Long.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                return input.readLong();
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                input.writeLong(((Long) value));
            }
        },
        FLOAT(0x07, Float.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                return input.readFloat();
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                input.writeFloat(((Float) value));
            }
        },
        DOUBLE(0x08, Double.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                return input.readDouble();
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                input.writeDouble(((Double) value));
            }
        },
        BIG_DECIMAL(0x09, BigDecimal.class) {
            private final static byte ZERO = 0x00;
            private final static byte ONE = 0x01;
            private final static byte SMALL_SCALE_0 = 0x10;
            private final static byte SMALL_SCALE_N = 0x11;

            private final static byte BIG_DECIMAL = 0x12;

            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                byte type = input.readByte();
                if (ZERO == type) {
                    return BigDecimal.ZERO;
                }
                if (ONE == type) {
                    return BigDecimal.ONE;
                }
                if (SMALL_SCALE_0 == type) {
                    return BigDecimal.valueOf(input.readLong());
                }
                if (SMALL_SCALE_N == type) {
                    int scale = input.readInt();
                    return BigDecimal.valueOf(input.readLong(), scale);
                }
                int scale = input.readInt();
                int len = input.readInt();
                byte[] bytes = new byte[len];
                input.read(bytes);
                BigInteger b = new BigInteger(bytes);
                return new BigDecimal(b, scale);
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                BigDecimal decimal = ((BigDecimal) value);

                if (BigDecimal.ZERO.equals(decimal)) {
                    input.write(ZERO);
                } else if (BigDecimal.ONE.equals(decimal)) {
                    input.write(ONE);
                } else {
                    int scale = decimal.scale();
                    BigInteger b = decimal.unscaledValue();
                    int bits = b.bitLength();
                    if (bits < 64) {
                        if (scale == 0) {
                            input.write(SMALL_SCALE_0);
                        } else {
                            input.write(SMALL_SCALE_N);
                            input.writeInt(scale);
                        }
                        input.writeLong(b.longValue());
                    } else {
                        byte[] bytes = b.toByteArray();
                        input.write(BIG_DECIMAL);
                        input.writeInt(scale);
                        input.writeInt(bytes.length);
                        input.write(bytes);
                    }
                }

            }
        },
        BIG_INTEGER(0x0A, BigInteger.class) {
            private final static byte ZERO = 0x00;
            private final static byte ONE = 0x01;
            private final static byte SMALL = 0x10;

            private final static byte BIG_INTEGER = 0x12;

            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                byte type = input.readByte();
                if (ZERO == type) {
                    return BigInteger.ZERO;
                }
                if (ONE == type) {
                    return BigInteger.ONE;
                }
                if (SMALL == type) {
                    return BigInteger.valueOf(input.readLong());
                }
                int len = input.readInt();
                byte[] bytes = new byte[len];
                input.read(bytes);
                return new BigInteger(bytes);
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                BigInteger b = ((BigInteger) value);

                if (BigInteger.ZERO.equals(b)) {
                    input.write(ZERO);
                } else if (BigInteger.ONE.equals(b)) {
                    input.write(ONE);
                } else {
                    int bits = b.bitLength();
                    if (bits < 64) {
                        input.write(SMALL);
                        input.writeLong(b.longValue());
                    } else {
                        byte[] bytes = b.toByteArray();
                        input.write(BIG_INTEGER);
                        input.writeInt(bytes.length);
                        input.write(bytes);
                    }
                }

            }
        },
        STRING(0x0B, String.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                return input.readUTF();
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                input.writeUTF(String.valueOf(value));
            }
        },
        ARRAY(0x0C, Object[].class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                Serializer serializer = all[input.readUnsignedByte()];
                int len = input.readInt();
                Object array = Array.newInstance(serializer.getJavaType(), len);
                for (int i = 0; i < len; i++) {
                    Array.set(array, i, SerializeUtils.readObject(input));
                }
                return array;
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                Class<?> type = value.getClass().getComponentType();
                Serializer serializer = lookup(type);
                input.writeByte(serializer.getCode());
                int len = Array.getLength(value);
                input.writeInt(len);

                for (int i = 0; i < len; i++) {
                    writeObject(Array.get(value, i), input);
                }

            }
        },
        MAP(0x0D, Map.class) {
            @Override
            Object read(ObjectInput input) {
                return SerializeUtils.readMap(input, Maps::newLinkedHashMapWithExpectedSize);
            }

            @Override
            void write(Object value, ObjectOutput input) {
                writeKeyValue(((Map) value), input);
            }
        },
        OBJECT(0x0E, Serializable.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                return input.readObject();
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                input.writeObject(value);
            }
        },
        LIST(0x0F, List.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                int len = input.readInt();
                List<Object> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    list.add(SerializeUtils.readObject(input));
                }
                return list;
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                Collection<?> list = ((Collection<?>) value);

                int len = list.size();
                input.writeInt(len);

                for (Object o : list) {
                    writeObject(o, input);
                }
            }
        },
        SET(0x13, Set.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                int len = input.readInt();
                Set<Object> list = Sets.newLinkedHashSetWithExpectedSize(len);
                for (int i = 0; i < len; i++) {
                    list.add(SerializeUtils.readObject(input));
                }
                return list;
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                Collection<?> list = ((Collection<?>) value);

                int len = list.size();
                input.writeInt(len);

                for (Object o : list) {
                    writeObject(o, input);
                }
            }
        },
        //ConcurrentSet
        C_SET(0x23, Set.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                int len = input.readInt();
                Set<Object> list = ConcurrentHashMap.newKeySet(len);
                for (int i = 0; i < len; i++) {
                    list.add(SerializeUtils.readObject(input));
                }
                return list;
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                Collection<?> list = ((Collection<?>) value);

                int len = list.size();
                input.writeInt(len);

                for (Object o : list) {
                    writeObject(o, input);
                }
            }
        },
        //ConcurrentMap
        C_MAP(0x20, ConcurrentMap.class) {
            @Override
            Object read(ObjectInput input) {
                return SerializeUtils.readMap(input, ConcurrentHashMap::new);
            }

            @Override
            void write(Object value, ObjectOutput input) {
                writeKeyValue(((Map) value), input);
            }
        },

        Netty(0x11, io.netty.buffer.ByteBuf.class) {
            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput output) {
                ByteBuf buf = ((ByteBuf) value);
                byte[] bytes = ByteBufUtil.getBytes(buf);
                ReferenceCountUtil.safeRelease(buf);

                output.writeInt(bytes.length);
                output.write(bytes);
            }

            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                int len = input.readInt();
                byte[] bytes = new byte[len];
                input.readFully(bytes);
                return Unpooled.wrappedBuffer(bytes);
            }
        },

        Nio(0x12, ByteBuffer.class) {
            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput output) {
                ByteBuffer buf = ((ByteBuffer) value);
                byte[] bytes;
                if (buf.hasArray()) {
                    bytes = buf.array();
                } else {
                    bytes = new byte[buf.remaining()];
                    buf.get(bytes);
                }
                output.writeInt(bytes.length);
                output.write(bytes);
            }

            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                int len = input.readInt();
                byte[] bytes = new byte[len];
                input.readFully(bytes);
                return ByteBuffer.wrap(bytes);
            }
        },

        MESSAGE(0x30, Message.class) {
            @Override
            void write(Object value, ObjectOutput output) {
                MessageType.writeExternal(((Message) value), output);
            }

            @Override
            Object read(ObjectInput input) {
                return MessageType.readExternal(input);
            }
        },

        JSON(0x10, Object.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                String clazz = input.readUTF();
                Class<?> tClass = SerializeUtils.getClass(clazz);
                int len = input.readInt();
                byte[] jsonByte = new byte[len];
                input.readFully(jsonByte);
                return com.alibaba.fastjson.JSON.parseObject(jsonByte, tClass);
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput output) {
                Class<?> clazz = ClassUtils.getUserClass(value);
                String className;
                //跨classloader,序列化为Object
                if (clazz.getClassLoader() != null
                    && clazz.getClassLoader() != ClassUtils.getDefaultClassLoader()) {
                    className = Object.class.getName();
                } else {
                    className = clazz.getName();
                }
                output.writeUTF(className);
                byte[] jsonBytes = com.alibaba.fastjson.JSON.toJSONBytes(value);
                output.writeInt(jsonBytes.length);
                output.write(jsonBytes);
            }
        },
        SharedPathStringSer(0x40, SharedPathString.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                int size = input.readInt();
                String[] arr = new String[size];
                for (int i = 0; i < size; i++) {
                    arr[i] = RecyclerUtils.intern(input.readUTF());
                }
                return SharedPathString.of(arr);
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                SharedPathString path = ((SharedPathString) value);
                input.writeInt(path.size());
                for (String str : path.unsafeSeparated()) {
                    input.writeUTF(str);
                }
            }
        },
        SeparatedCharSequenceSer(0x41, SeparatedCharSequence.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                char c = input.readChar();
                int size = input.readInt();
                String[] arr = new String[size];
                for (int i = 0; i < size; i++) {
                    arr[i] = input.readUTF();
                }
                return SeparatedString.create(c, arr);
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                SeparatedCharSequence path = ((SeparatedCharSequence) value);
                input.writeChar(path.separator());
                input.writeInt(path.size());
                for (CharSequence str : path) {
                    input.writeUTF(str.toString());
                }
            }
        },
        StackTraceElement(0x42, java.lang.StackTraceElement.class) {
            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                String className = input.readUTF();
                String methodName = input.readUTF();

                String fileName = SerializeUtils.readNullableUTF(input);

                int lineNumber = input.readInt();
                return new StackTraceElement(className, methodName, fileName, lineNumber);
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                StackTraceElement element = ((StackTraceElement) value);
                input.writeUTF(element.getClassName());
                input.writeUTF(element.getMethodName());

                SerializeUtils.writeNullableUTF(element.getFileName(), input);

                input.writeInt(element.getLineNumber());
            }
        };

        public final int code;
        public final Class<?> javaType;

        abstract Object read(ObjectInput input);

        abstract void write(Object value, ObjectOutput input);

        @Override
        public void serialize(Object value, ObjectOutput input) {
            write(value, input);
        }

        @Override
        public Object deserialize(ObjectInput input) {
            return read(input);
        }
    }

    @Getter
    @AllArgsConstructor
    static class ExternalSerializer implements Serializer {
        private final int code;
        private final Class<? extends Externalizable> javaType;
        private final Function<ObjectInput, ? extends Externalizable> newInstance;

        @Override
        @SneakyThrows
        public Object deserialize(ObjectInput input) {
            Externalizable exz = newInstance.apply(input);
            exz.readExternal(input);
            return exz;
        }

        @Override
        @SneakyThrows
        public void serialize(Object value, ObjectOutput input) {
            Externalizable exz = ((Externalizable) value);
            exz.writeExternal(input);
        }
    }

    public interface Serializer {
        int getCode();

        Class<?> getJavaType();

        Object deserialize(ObjectInput input);

        void serialize(Object value, ObjectOutput output);
    }
}
