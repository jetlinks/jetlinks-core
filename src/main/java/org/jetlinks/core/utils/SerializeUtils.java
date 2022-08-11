package org.jetlinks.core.utils;

import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SerializeUtils {

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
        Type type;
        if (obj == null) {
            type = Type.NULL;
        } else {
            type = Type.of(obj);
        }
        out.writeByte(type.code);
        type.write(obj, out);
    }

    @SneakyThrows
    public static Object readObject(ObjectInput input) {
        Type type = Type.all[input.readByte()];
        return type.read(input);
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

    @AllArgsConstructor
    private enum Type {
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
                Type elementType = Type.all[input.readByte()];
                int len = input.readInt();
                Object array = Array.newInstance(elementType.javaType, len);
                for (int i = 0; i < len; i++) {
                    Array.set(array, i, SerializeUtils.readObject(input));
                }
                return array;
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput input) {
                Class<?> type = value.getClass().getComponentType();
                Type elementType = Type.of(type);
                input.writeByte(elementType.ordinal());
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
        COLLECTION(0x0F, List.class) {
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
                List<?> list = ((List<?>) value);

                int len = list.size();
                input.writeInt(len);

                for (Object o : list) {
                    writeObject(o, input);
                }
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


        JSON(0x10, Object.class) {
            private final Map<String, Class<?>> clazzCache = new ConcurrentReferenceHashMap<>();

            @SneakyThrows
            private Class<?> loadClass(String name) {
                return SerializeUtils.class.getClassLoader().loadClass(name);
            }

            @Override
            @SneakyThrows
            Object read(ObjectInput input) {
                String clazz = input.readUTF();
                Class<?> tClass = clazzCache.computeIfAbsent(clazz, this::loadClass);
                int len = input.readInt();
                byte[] jsonByte = new byte[len];
                input.readFully(jsonByte);
                return com.alibaba.fastjson.JSON.parseObject(jsonByte, tClass);
            }

            @Override
            @SneakyThrows
            void write(Object value, ObjectOutput output) {
                output.writeUTF(value.getClass().getName());
                byte[] jsonBytes = com.alibaba.fastjson.JSON.toJSONBytes(value);
                output.writeInt(jsonBytes.length);
                output.write(jsonBytes);
            }
        };

        private final int code;
        private final Class<?> javaType;

        abstract Object read(ObjectInput input);

        abstract void write(Object value, ObjectOutput input);

        final static Type[] all;

        static {
            all = new Type[0xff];
            for (Type value : values()) {
                all[value.code] = value;
            }
        }

        private static final Map<Class<?>, Type> cache = new ConcurrentReferenceHashMap<>();

        public static Type of(Object javaType) {
            if (javaType instanceof String) {
                return STRING;
            }
            if (javaType instanceof Boolean) {
                return BOOLEAN;
            }
            if (javaType instanceof Integer) {
                return INT;
            }
            if (javaType instanceof Long) {
                return LONG;
            }
            if (javaType instanceof Double) {
                return DOUBLE;
            }
            if (javaType instanceof Float) {
                return FLOAT;
            }
            if (javaType instanceof Byte) {
                return BYTE;
            }
            if (javaType instanceof Short) {
                return SHORT;
            }
            if (javaType instanceof Character) {
                return CHAR;
            }
            if (javaType instanceof BigDecimal) {
                return BIG_DECIMAL;
            }
            if (javaType instanceof BigInteger) {
                return BIG_INTEGER;
            }
            if (javaType instanceof Map) {
                return MAP;
            }
            if (javaType instanceof List) {
                return COLLECTION;
            }
            if (javaType instanceof ByteBuf) {
                return Netty;
            }
            if (javaType instanceof ByteBuffer) {
                return Nio;
            }
            return of(javaType.getClass());
        }

        public static Type of(Class<?> javaType) {
            return cache.computeIfAbsent(javaType, t -> {
                if (t.isPrimitive()) {
                    t = Primitives.wrap(t);
                }
                for (Type type : all) {
                    if (type.javaType == t || type.javaType.isAssignableFrom(t)) {
                        return type;
                    }
                }
                if (t.isArray() && !t.getComponentType().isPrimitive()) {
                    return ARRAY;
                }
                return JSON;
            });
        }

    }

}
