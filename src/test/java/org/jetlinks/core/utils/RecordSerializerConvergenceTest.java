package org.jetlinks.core.utils;

import org.junit.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.*;

public class RecordSerializerConvergenceTest {
    private record Item(String id) { }
    private record Batch(Item[] items) { }
    private record Fatal(boolean fail) {
        private Fatal { if (fail) throw new LinkageError("constructor-fatal"); }
    }
    private enum State { ready, stopped }
    private static class Bean implements Serializable {
        private final String id;
        private Bean(String id) { this.id = id; }
    }
    public record ForeignRecord(String id) { }

    private interface Writer { void write(ObjectOutput output) throws Exception; }

    private static byte[] encode(Writer writer) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            writer.write(output);
            output.flush();
        }
        return bytes.toByteArray();
    }

    private static Object decode(byte[] bytes) throws Exception {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return SerializeUtils.readObject(input);
        }
    }

    private static Object copy(Object value) throws Exception {
        return decode(encode(output -> SerializeUtils.writeObject(value, output)));
    }

    @Test
    public void propagatesFatalConstructorFailures() {
        LinkageError error = assertThrows(LinkageError.class,
            () -> RecordMetadata.of(Fatal.class).newInstance(new Object[]{true}));
        assertEquals("constructor-fatal", error.getMessage());
    }

    @Test
    public void preservesDeclaredReferenceArrayTypes() throws Exception {
        Item item = new Item("point");
        Item[] records = (Item[]) copy(new Item[]{item, null, item});
        assertSame(records[0], records[2]);
        assertEquals(item, records[0]);
        assertNull(records[1]);

        assertArrayEquals(new State[]{State.ready, State.stopped},
                          (State[]) copy(new State[]{State.ready, State.stopped}));
        Bean[] beans = (Bean[]) copy(new Bean[]{new Bean("bean")});
        assertEquals("bean", beans[0].id);
    }

    @Test
    public void preservesEmptyNullAndMultidimensionalArrayTypes() throws Exception {
        assertEquals(Item[].class, copy(new Item[0]).getClass());
        Item[] nulls = (Item[]) copy(new Item[2]);
        assertEquals(2, nulls.length);
        assertNull(nulls[0]);
        Item[][] records = (Item[][]) copy(new Item[][]{new Item[]{new Item("nested")}, null});
        assertEquals(new Item("nested"), records[0][0]);
        assertNull(records[1]);
        int[][] numbers = (int[][]) copy(new int[][]{new int[]{1, 2}, new int[0]});
        assertArrayEquals(new int[]{1, 2}, numbers[0]);
        assertArrayEquals(new int[0], numbers[1]);
    }

    @Test
    public void constructsRecordsWithConcreteArrayComponents() throws Exception {
        Item item = new Item("point");
        Batch result = (Batch) copy(new Batch(new Item[]{item, item}));
        assertArrayEquals(new Item[]{item, item}, result.items());
        assertSame(result.items()[0], result.items()[1]);
    }

    @Test
    public void retainsExistingScalarArrayEncoding() throws Exception {
        String[] source = {"first", null, "last"};
        byte[] legacy = encode(output -> {
            output.writeByte(0x0c);
            output.writeByte(0x0b);
            output.writeInt(source.length);
            for (String value : source) SerializeUtils.writeObject(value, output);
        });
        assertArrayEquals(legacy, encode(output -> SerializeUtils.writeObject(source, output)));
        assertArrayEquals(source, (String[]) decode(legacy));
    }

    @Test
    public void rejectsDuplicateUnknownComponents() throws Exception {
        byte[] bytes = encode(output -> {
            output.writeByte(0x24);
            output.writeInt(1);
            output.writeInt(-1);
            output.writeUTF(Item.class.getName());
            output.writeInt(2);
            output.writeUTF("future");
            output.writeUTF("future");
            SerializeUtils.writeObject("first", output);
            SerializeUtils.writeObject("second", output);
        });
        assertThrows(StreamCorruptedException.class, () -> decode(bytes));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void avoidsRetainingUnregisteredForeignClasses() throws Exception {
        String name = ForeignRecord.class.getName();
        byte[] bytes;
        try (InputStream input = ForeignRecord.class.getResourceAsStream(
            "RecordSerializerConvergenceTest$ForeignRecord.class")) {
            assertNotNull(input);
            bytes = input.readAllBytes();
        }
        ClassLoader loader = new ClassLoader(ForeignRecord.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String type, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(type)) return super.loadClass(type, resolve);
                synchronized (getClassLoadingLock(type)) {
                    Class<?> result = findLoadedClass(type);
                    if (result == null) result = defineClass(type, bytes, 0, bytes.length);
                    if (resolve) resolveClass(result);
                    return result;
                }
            }
        };
        Class<?> foreign = loader.loadClass(name);
        assertEquals(0x10, SerializeUtils.lookup(foreign).getCode());
        Field cacheField = SerializeUtils.class.getDeclaredField("cache");
        assertTrue(cacheField.trySetAccessible());
        Map<Class<?>, ?> cache = (Map<Class<?>, ?>) cacheField.get(null);
        assertFalse("Core must not retain a foreign Class for the JSON fallback", cache.containsKey(foreign));

        SerializeUtils.Serializer explicit = new SerializeUtils.Serializer() {
            public int getCode() { return 0x7b; }
            public Class<?> getJavaType() { return foreign; }
            public Object deserialize(ObjectInput input) { throw new UnsupportedOperationException(); }
            public void serialize(Object value, ObjectOutput output) { throw new UnsupportedOperationException(); }
        };
        SerializeUtils.registerSerializer(explicit);
        assertSame(explicit, SerializeUtils.lookup(foreign));
    }

    @Test
    public void rejectsInvalidTypedArraysWithoutAllocatingFromAnUntrustedLength() throws Exception {
        assertThrows(StreamCorruptedException.class, () -> decode(encode(output -> {
            output.writeByte(0x25); output.writeUTF(String.class.getName()); output.writeInt(-1);
        })));
        assertThrows(EOFException.class, () -> decode(encode(output -> {
            output.writeByte(0x25); output.writeUTF(String.class.getName()); output.writeInt(Integer.MAX_VALUE);
        })));
        assertThrows(InvalidObjectException.class, () -> decode(encode(output -> {
            output.writeByte(0x25); output.writeUTF(String.class.getName()); output.writeInt(1);
            SerializeUtils.writeObject(42, output);
        })));
    }
}
