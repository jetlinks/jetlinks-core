package org.jetlinks.core.utils;

import org.junit.Test;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class RecordSerializerTest {
    private record Leaf(String id, long epoch, BigDecimal decimal) implements Serializable { }
    private record Node(Leaf leaf, Map<String, Leaf> values, List<Leaf> history) { }
    private record Adjusted(int value) { @Override public int value() { return value * 2; } }
    private record Evolving(String id, int count, boolean active, char marker, String extra) { }
    private record Nullable(String id, List<String> items, byte[] bytes) { }
    private record Validated(int count) {
        private Validated { if (count < 0) throw new IllegalArgumentException("negative"); }
    }
    private record Loop(List<Object> links) { }
    private record Reentrant(Leaf value) implements Serializable { }
    private record Outer(Leaf before, Reentrant nested, Leaf after) { }
    public record PluginRecord(String id) implements Serializable {
        public String getId() { return id; }
    }
    private interface Writer { void write(ObjectOutput output) throws Exception; }

    private static byte[] wire(Writer writer) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) {
            writer.write(output);
            output.flush();
        }
        return bytes.toByteArray();
    }
    private static byte[] encode(Object value) throws Exception {
        return wire(output -> SerializeUtils.writeObject(value, output));
    }
    private static Object decode(byte[] bytes) throws Exception {
        try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return SerializeUtils.readObject(input);
        }
    }
    private static void definition(ObjectOutput output, Class<?> type, Map<String, Object> fields) throws Exception {
        output.writeByte(RecordSerializer.CODE);
        output.writeInt(1);
        output.writeUTF(type.getName());
        output.writeInt(fields.size());
        for (var field : fields.entrySet()) {
            output.writeUTF(field.getKey());
            SerializeUtils.writeObject(field.getValue(), output);
        }
    }
    private static Leaf leaf() {
        return new Leaf(null, 9007199254740993L, new BigDecimal("12345678901234567890.01234567890123456789"));
    }

    @Test public void supportsNestedNonSerializableRecordsAndSharedReferences() throws Exception {
        Leaf leaf = leaf();
        Node source = new Node(leaf, Map.of("value", leaf), List.of(leaf));
        Node restored = (Node) decode(encode(source));
        assertEquals(source, restored);
        assertSame(restored.leaf(), restored.values().get("value"));
        assertSame(restored.leaf(), restored.history().get(0));
        assertEquals(RecordSerializer.CODE, SerializeUtils.lookup(leaf).getCode());
    }
    @Test public void preservesReferencesWhenRootIsCollection() throws Exception {
        Leaf value = leaf();
        List<?> restored = (List<?>) decode(encode(List.of(value, value)));
        assertSame(restored.get(0), restored.get(1));
    }
    @Test public void preservesOriginalComponentsInsteadOfAccessors() throws Exception {
        assertEquals(new Adjusted(3), decode(encode(new Adjusted(3))));
    }
    @Test public void preservesNullEmptyCollectionsAndBinary() throws Exception {
        var source = new Nullable(null, List.of(), new byte[]{0, 1, -1});
        Nullable restored = (Nullable) decode(encode(source));
        assertNull(restored.id());
        assertEquals(List.of(), restored.items());
        assertArrayEquals(source.bytes(), restored.bytes());
        assertNull(((Nullable) decode(encode(new Nullable(null, null, null)))).items());
    }
    @Test public void matchesNamesAndDefaultsMissingComponents() throws Exception {
        var fields = new LinkedHashMap<String, Object>();
        fields.put("active", true);
        fields.put("id", "node");
        Evolving restored = (Evolving) decode(wire(out -> definition(out, Evolving.class, fields)));
        assertEquals(new Evolving("node", 0, true, '\0', null), restored);
    }
    @Test public void consumesUnknownComponentsAndPreservesFollowingData() throws Exception {
        byte[] bytes = wire(out -> {
            definition(out, Evolving.class, Map.of("future", List.of("unknown"), "id", "node"));
            out.writeUTF("END");
        });
        try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            assertEquals(new Evolving("node", 0, false, '\0', null), SerializeUtils.readObject(input));
            assertEquals("END", input.readUTF());
        }
    }
    @Test public void rejectsIncompatibleComponentType() throws Exception {
        assertThrows(InvalidObjectException.class,
            () -> decode(wire(out -> definition(out, Evolving.class, Map.of("count", "bad")))));
        var nullPrimitive = new HashMap<String, Object>();
        nullPrimitive.put("count", null);
        assertThrows(InvalidObjectException.class,
            () -> decode(wire(out -> definition(out, Evolving.class, nullPrimitive))));
    }
    @Test public void invokesCanonicalValidation() throws Exception {
        InvalidObjectException error = assertThrows(InvalidObjectException.class,
            () -> decode(wire(out -> definition(out, Validated.class, Map.of("count", -1)))));
        assertEquals("negative", error.getCause().getMessage());
    }
    @Test public void rejectsNegativeCountDuplicateNamesAndNonRecordTypes() throws Exception {
        assertThrows(StreamCorruptedException.class, () -> decode(wire(out -> {
            out.writeByte(RecordSerializer.CODE); out.writeInt(1);
            out.writeUTF(Evolving.class.getName()); out.writeInt(-1);
        })));
        assertThrows(StreamCorruptedException.class, () -> decode(wire(out -> {
            out.writeByte(RecordSerializer.CODE); out.writeInt(1);
            out.writeUTF(Evolving.class.getName()); out.writeInt(2);
            out.writeUTF("id"); SerializeUtils.writeObject("first", out);
            out.writeUTF("id"); SerializeUtils.writeObject("second", out);
        })));
        assertThrows(InvalidObjectException.class,
            () -> decode(wire(out -> definition(out, String.class, Map.of()))));
    }
    @Test public void rejectsUnknownCodesInsteadOfReturningNull() throws Exception {
        assertThrows(StreamCorruptedException.class, () -> decode(wire(out -> out.writeByte(0xfc))));
        assertThrows(StreamCorruptedException.class, () -> decode(wire(out -> {
            out.writeByte(0x0c); out.writeByte(0xfc); out.writeInt(0);
        })));
        assertNull(decode(encode(null)));
    }
    @Test public void rejectsInvalidRecordHandles() throws Exception {
        for (int id : new int[]{0, 2, -1, Integer.MIN_VALUE}) {
            assertThrows(StreamCorruptedException.class, () -> decode(wire(out -> {
                out.writeByte(RecordSerializer.CODE); out.writeInt(id);
            })));
        }
    }
    @Test public void rejectsCyclicRecordWithoutRecursingUntilOverflow() throws Exception {
        var values = new ArrayList<Object>();
        var source = new Loop(values);
        values.add(source);
        assertThrows(InvalidObjectException.class, () -> encode(source));
        assertEquals(leaf(), decode(encode(leaf())));
    }
    @Test public void failedStreamDoesNotRetainTheFrameOrPoisonNextCall() throws Exception {
        Leaf source = leaf();
        try (var output = new ObjectOutputStream(new ByteArrayOutputStream()) {
            @Override public void writeInt(int value) throws IOException { throw new IOException("broken"); }
        }) {
            assertThrows(IOException.class, () -> SerializeUtils.writeObject(source, output));
        }
        assertEquals(source, decode(encode(source)));
        assertThrows(EOFException.class, () -> decode(wire(out -> {
            out.writeByte(RecordSerializer.CODE); out.writeInt(1); out.writeUTF(Leaf.class.getName()); out.writeInt(1);
        })));
        assertEquals(source, decode(encode(source)));
    }
    @Test public void respectsExplicitSerializerAndNestedIndependentStreams() throws Exception {
        SerializeUtils.registerSerializer(new SerializeUtils.Serializer() {
            public int getCode() { return 0x7a; }
            public Class<?> getJavaType() { return Reentrant.class; }
            public void serialize(Object value, ObjectOutput output) {
                try {
                    byte[] bytes = encode(((Reentrant) value).value());
                    output.writeInt(bytes.length); output.write(bytes);
                } catch (Exception error) { throw new IllegalStateException(error); }
            }
            public Object deserialize(ObjectInput input) {
                try {
                    byte[] bytes = new byte[input.readInt()]; input.readFully(bytes);
                    return new Reentrant((Leaf) decode(bytes));
                } catch (Exception error) { throw new IllegalStateException(error); }
            }
        });
        Leaf value = leaf();
        Outer source = new Outer(value, new Reentrant(value), value);
        Outer restored = (Outer) decode(encode(source));
        assertEquals(source, restored);
        assertSame(restored.before(), restored.after());
        assertNotSame(restored.before(), restored.nested().value());
    }
    @Test public void retainsCrossLoaderSafetyPolicy() throws Exception {
        String name = PluginRecord.class.getName();
        byte[] bytes;
        try (InputStream stream = PluginRecord.class.getResourceAsStream("RecordSerializerTest$PluginRecord.class")) {
            bytes = Objects.requireNonNull(stream).readAllBytes();
        }
        ClassLoader loader = new ClassLoader(PluginRecord.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String type, boolean resolve) throws ClassNotFoundException {
                if (!type.equals(name)) return super.loadClass(type, resolve);
                synchronized (getClassLoadingLock(type)) {
                    Class<?> loaded = findLoadedClass(type);
                    if (loaded == null) loaded = defineClass(type, bytes, 0, bytes.length);
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
            }
        };
        Object source = loader.loadClass(name).getConstructor(String.class).newInstance("plugin");
        assertEquals(0x10, SerializeUtils.lookup(source).getCode());
        assertEquals("plugin", ((Map<?, ?>) decode(encode(source))).get("id"));
    }
    @Test public void isolatesConcurrentFrames() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Object>> calls = new ArrayList<>();
            for (int i = 0; i < 64; i++) calls.add(() -> decode(encode(leaf())));
            for (Future<Object> result : executor.invokeAll(calls)) assertEquals(leaf(), result.get());
        } finally { executor.shutdownNow(); }
    }
}
