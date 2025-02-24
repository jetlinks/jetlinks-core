package org.jetlinks.core.utils;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

@Slf4j
class TypedCollectionSerializer implements SerializeUtils.Serializer {

    public static final int CODE = 0x22;


    @Override
    public int getCode() {
        return CODE;
    }

    @Override
    public Class<?> getJavaType() {
        return Collection.class;
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("all")
    public Object deserialize(ObjectInput input) {
        int type = input.readByte();
        int size;
        Collection<Object> instance;
        if (type == -1) {
            String name = input.readUTF();
            size = input.readInt();
            Class<Collection<Object>> tClass = SerializeUtils.getClass(name);
            instance = tClass.getConstructor().newInstance();
        } else if (type == -2) {
            return input.readObject();
        } else {
            size = input.readInt();
            instance = CollectionType.VALUES[type].instance.apply(size);
        }
        if (size == 0) {
            return instance;
        }
        for (int i = 0; i < size; i++) {
            instance.add(SerializeUtils.readObject(input));
        }
        return instance;

    }

    @Override
    @SneakyThrows
    public void serialize(Object value, ObjectOutput output) {
        @SuppressWarnings("all")
        Collection<?> c = ((Collection<?>) value);

        Class<?> clazz = value.getClass();
        CollectionType type = CollectionType.findOrNull(clazz);
        if (type == null) {
            if (value instanceof Serializable) {
                output.writeByte(-2);
                output.writeObject(value);
                return;
            }
            if (value instanceof Set) {
                type = CollectionType.hashSet;
            } else {
                type = CollectionType.arrayList;
            }
        }
        output.writeByte(type.ordinal());
        output.writeInt(c.size());
        for (Object o : c) {
            SerializeUtils.writeObject(o, output);
        }
    }

    @AllArgsConstructor
    enum CollectionType {
        //list
        arrayList(ArrayList::new, ArrayList.class),
        @SuppressWarnings("all")
        arrayListC(ArrayList::new, Arrays.asList(0).getClass()),
        linkedList(ignore -> new LinkedList<>(), LinkedList.class),
        cpList(ignore -> new CopyOnWriteArrayList<>(), CopyOnWriteArrayList.class),
        vector(Vector::new, Vector.class),
        //set
        hashSet(Sets::newHashSetWithExpectedSize, HashSet.class),
        linkedHashSet(Sets::newLinkedHashSetWithExpectedSize, LinkedHashSet.class),
        concurrentHashSet(ConcurrentHashMap::newKeySet, ConcurrentHashMap.KeySetView.class),
        treeSet(ignore -> new TreeSet<>(), TreeSet.class),
        cpSet(ignore -> new CopyOnWriteArraySet<>(), CopyOnWriteArraySet.class),


        //java Collections
        emptySet(p -> Collections.emptySet(), Collections.emptySet().getClass()),
        @SuppressWarnings("all")
        unmodifiableSet(Sets::newHashSetWithExpectedSize, Collections
            .unmodifiableSet(Collections.emptySet())
            .getClass()),

        //guava
        guavaTransformRandomAccessList(Lists::newArrayListWithCapacity, Lists
            .transform(Collections.emptyList(), x -> x)
            .getClass()),
        guavaTransformList(ignore -> new LinkedList<>(), Lists
            .transform(new LinkedList<>(), x -> x)
            .getClass()),

        guavaFilteredSet(Sets::newHashSetWithExpectedSize, Sets.filter(Collections.emptySet(), e -> true).getClass()),
        guavaNavigableSet(Sets::newHashSetWithExpectedSize, Sets.filter(Sets.newTreeSet(), e -> true).getClass()),
        @SuppressWarnings("all")
        guavaSortedSet(Sets::newHashSetWithExpectedSize, Sets
            .filter((SortedSet) Sets.newTreeSet(), e -> true)
            .getClass()),

        guavaFilterCollection(ArrayList::new, Collections2.filter(Collections.emptySet(), e -> true).getClass()),
        guavaTransformedCollection(ArrayList::new, Collections2
            .transform(Collections.emptySet(), e -> true)
            .getClass()),

        ;
        private final Function<Integer, Collection<Object>> instance;
        private final Class<?> clazz;
        private static final CollectionType[] VALUES = CollectionType.values();

        static CollectionType findOrNull(Class<?> clazz) {
            for (CollectionType value : VALUES) {
                if (value.clazz == clazz) {
                    return value;
                }
            }

            return null;
        }

    }
}
