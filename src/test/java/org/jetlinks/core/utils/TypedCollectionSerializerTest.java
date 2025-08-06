package org.jetlinks.core.utils;

import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Objects;

import static org.junit.Assert.*;

public class TypedCollectionSerializerTest {


    @Test
    @SneakyThrows
    public void testCol() {
        TypedCollectionSerializer serializer = new TypedCollectionSerializer();

        ByteArrayOutputStream s = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(s);
        serializer.serialize(Maps.filterValues(new HashMap<>(), Objects::nonNull).values(), out);
        out.close();

        Object obj =  serializer.deserialize(new ObjectInputStream(new ByteArrayInputStream(s.toByteArray())));
        assertNotNull(obj);
        System.out.println(obj.getClass());
    }
}