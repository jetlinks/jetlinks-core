package org.jetlinks.core.utils;

import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CompositeSetTest {

    @Test
    @SneakyThrows
    public void testSerializable() {
        CompositeSet<String> set = new CompositeSet<>(
                Sets.newHashSet("a", "b", "c"),
                Sets.newHashSet("a", "b", "c")
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream oout = new ObjectOutputStream(out)) {
            oout.writeObject(set);
        }

        try (ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            Object read = oin.readObject();
            assertTrue(read instanceof Set);
            System.out.println(read);
        }
    }

    @Test
    @SneakyThrows
    public void test() {
        {
            CompositeSet<String> set = new CompositeSet<>(
                    Sets.newHashSet("a", "b", "c"),
                    Sets.newHashSet("a", "b", "c")
            );

            assertEquals(3, set.size());
            assertEquals(3, new ArrayList<>(set).size());
            System.out.println(set);

        }

        {
            CompositeSet<String> set = new CompositeSet<>(
                    Sets.newHashSet("a", "b", "c"),
                    Sets.newHashSet("a", "b", "c", "d")
            );

            assertEquals(4, set.size());
            assertEquals(4, new ArrayList<>(set).size());
            assertEquals(4, set.toArray(new String[0]).length);
            System.out.println(set);

        }

        {
            CompositeSet<String> set = new CompositeSet<>(
                    Sets.newHashSet("a", "b", "c", "d"),
                    Sets.newHashSet("a", "b", "c")
            );

            assertEquals(4, set.size());
            assertEquals(4, new ArrayList<>(set).size());
            assertEquals(4, set.toArray(new String[0]).length);
            System.out.println(set);

        }

        {
            CompositeSet<String> set = new CompositeSet<>(
                    Sets.newHashSet("a", "b", "c"),
                    Sets.newHashSet("1", "2", "3")
            );

            assertEquals(6, set.size());
            assertEquals(6, new ArrayList<>(set).size());
            System.out.println(set);

        }
    }
}