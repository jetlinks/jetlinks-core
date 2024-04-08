package org.jetlinks.core.utils;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;

@Slf4j
class TypedMapSerializer implements SerializeUtils.Serializer {

    public static final int CODE = 0x21;


    @Override
    public int getCode() {
        return CODE;
    }

    @Override
    public Class<?> getJavaType() {
        return Map.class;
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("all")
    public Object deserialize(ObjectInput input) {
        int type = input.readByte();
        if (type == -1) {
            String name = input.readUTF();
            Map<?, ?> instance;
            try {
                Class<Map<?, ?>> tClass = SerializeUtils.getClass(name);
                instance = tClass.getConstructor().newInstance();
            } catch (Throwable e) {
                log.warn("can not create instance for class:{}", name, e);
                instance = Maps.newHashMap();
            }
            Map<?, ?> fInstance = instance;
            SerializeUtils.readMap(input, i -> fInstance);
            return instance;
        } else if (type == -2) {
            return input.readObject();
        }
        return SerializeUtils.readMap(input, (Function) MapType.VALUES[type].instance);

    }

    @Override
    @SneakyThrows
    public void serialize(Object value, ObjectOutput input) {
        @SuppressWarnings("all")
        Map<?, ?> map = ((Map<?, ?>) value);

        Class<?> clazz = value.getClass();
        MapType type = MapType.findOrNull(clazz);
        if (type == null) {
            if (map instanceof Serializable) {
                input.writeByte(-2);
                input.writeObject(map);
                return;
            }
            input.writeByte(-1);
            input.writeUTF(clazz.getName());
        } else {
            input.writeByte(type.ordinal());
        }

        SerializeUtils.writeKeyValue(map, input);
    }

    @AllArgsConstructor
    enum MapType {
        hashMap(Maps::newHashMapWithExpectedSize, HashMap.class),
        linkedHashMap(Maps::newLinkedHashMapWithExpectedSize, LinkedHashMap.class),
        concurrentHashMap(ConcurrentHashMap::new, ConcurrentHashMap.class),
        identityHashMap(IdentityHashMap::new,IdentityHashMap.class),
        treeMap(ignore -> new TreeMap<>(), TreeMap.class),
        concurrentSkipListMap(ignore -> new ConcurrentSkipListMap<>(), ConcurrentSkipListMap.class),
        hashTable(ignore -> new Hashtable<>(), Hashtable.class)

        //
        ;
        private final Function<Integer, Map<?, ?>> instance;
        private final Class<?> clazz;
        private static final MapType[] VALUES = MapType.values();

        static MapType findOrNull(Class<?> clazz) {
            for (MapType value : VALUES) {
                if (value.clazz == clazz) {
                    return value;
                }
            }

            //guava or Collections
            String clazzName = clazz.getName();
            if (clazzName.startsWith("java.util.Collections") ||
                clazzName.startsWith("com.google")) {
                return hashMap;
            }

            return null;
        }

    }
}
