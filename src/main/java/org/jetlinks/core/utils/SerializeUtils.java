package org.jetlinks.core.utils;

import lombok.SneakyThrows;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
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
    public static Object readNullableObject(ObjectInput in) {
        if (in.readBoolean()) {
            return null;
        }
        return in.readObject();
    }

    @SneakyThrows
    public static void writeNullableObject(Object str, ObjectOutput out) {
        if (str == null) {
            out.writeBoolean(true);
            return;
        }
        out.writeBoolean(false);
        out.writeObject(str);
    }

    @SneakyThrows
    public static <T> Map<String, T> readMap(ObjectInput in,
                                              Function<Integer, Map<String, T>> mapBuilder) {
        //header
        int headerSize = in.readInt();
        Map<String, T> map = mapBuilder.apply(Math.min(8, headerSize));

        for (int i = 0; i < headerSize; i++) {
            String key = in.readUTF();
            Object value = in.readObject();
            map.put(key, (T)value);
        }
        return map;
    }

    @SneakyThrows
    public static void readKeyValue(ObjectInput in,
                                    BiConsumer<String, Object> consumer) {
        //header
        int headerSize = in.readInt();
        for (int i = 0; i < headerSize; i++) {
            String key = in.readUTF();
            Object value =in.readObject();
            consumer.accept(key, value);
        }
    }

    @SneakyThrows
    public static void writeKeyValue(Map<String, ?> map, ObjectOutput out) {
        if (map == null) {
            out.writeInt(0);
        } else {
            out.writeInt(map.size());
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeObject(entry.getValue());
            }
        }
    }

    @SneakyThrows
    public static <T> void writeKeyValue(Collection<T> collection,
                                         Function<T, String> keyMapper,
                                         Function<T, Object> valueMapper,
                                         ObjectOutput out) {
        if (collection == null) {
            out.writeInt(0);
        } else {
            out.writeInt(collection.size());
            for (T t : collection) {
                String key = keyMapper.apply(t);
                Object value = valueMapper.apply(t);
                out.writeUTF(key);
                out.writeObject(value);
            }
        }
    }
}
