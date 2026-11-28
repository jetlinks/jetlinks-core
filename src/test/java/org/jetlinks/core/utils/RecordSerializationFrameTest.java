package org.jetlinks.core.utils;

import lombok.SneakyThrows;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class RecordSerializationFrameTest {
    private record Value(String id) { }
    private record Bridge(Value first, Value second) { }

    private interface Writer {
        void write(ObjectOutput output) throws Exception;
    }

    private static byte[] wire(Writer writer) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutput output = new ObjectOutputStream(bytes)) {
            writer.write(output);
        }
        return bytes.toByteArray();
    }

    private static Object copy(Object value) throws Exception {
        byte[] bytes = wire(output -> SerializeUtils.writeObject(value, output));
        try (ObjectInput input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return SerializeUtils.readObject(input);
        }
    }

    @Test
    public void keepsRecordWireBytes() throws Exception {
        Value value = new Value("point");
        byte[] expected = wire(output -> {
            output.writeByte(0x24);
            output.writeInt(1);
            output.writeInt(-1);
            output.writeUTF(Value.class.getName());
            output.writeInt(1);
            output.writeUTF("id");
            SerializeUtils.writeObject(value.id(), output);
        });
        assertArrayEquals(expected, wire(output -> SerializeUtils.writeObject(value, output)));
    }

    @Test
    public void directRecordSerializerKeepsIndependentFramesAndCallerStreamOpen() throws Exception {
        Value value = new Value("point");
        SerializeUtils.Serializer codec = SerializeUtils.lookup(Value.class);
        byte[] bytes = wire(output -> {
            codec.serialize(value, output);
            codec.serialize(value, output);
            output.writeUTF("END");
        });
        try (ObjectInput input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object first = codec.deserialize(input);
            Object second = codec.deserialize(input);
            assertEquals(value, first);
            assertEquals(value, second);
            assertNotSame(first, second);
            assertEquals("END", input.readUTF());
        }
    }

    @Test
    public void carriesReferencesAcrossSequentialThreadHandoffsWithinOneCall() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            SerializeUtils.registerSerializer(new SerializeUtils.Serializer() {
                public int getCode() { return 0x7c; }
                public Class<?> getJavaType() { return Bridge.class; }

                @SneakyThrows
                public void serialize(Object value, ObjectOutput output) {
                    Bridge bridge = (Bridge) value;
                    SerializeUtils.writeObject(bridge.first(), output);
                    worker.submit(() -> SerializeUtils.writeObject(bridge.second(), output))
                          .get(5, TimeUnit.SECONDS);
                }

                @SneakyThrows
                public Object deserialize(ObjectInput input) {
                    Value first = SerializeUtils.readObjectAs(input);
                    Value second = worker.submit(() -> (Value) SerializeUtils.readObject(input))
                                         .get(5, TimeUnit.SECONDS);
                    return new Bridge(first, second);
                }
            });
            Value value = new Value("point");
            Bridge restored = (Bridge) copy(new Bridge(value, value));
            assertEquals(value, restored.first());
            assertSame(restored.first(), restored.second());
        } finally {
            worker.shutdownNow();
        }
    }

    @Test
    public void nestedCallsAndFailuresReleaseAllReferencesBeforeFrameReuse() {
        Scheduler scheduler = Schedulers.newSingle("record-frame-test");
        try {
            Mono.fromCallable(() -> {
                assertTrue(Schedulers.isInNonBlockingThread());
                CapturingCodec codec = new CapturingCodec();
                Object previousOutput = null;
                Object previousInput = null;
                for (boolean fail : new boolean[]{false, true, false}) {
                    codec.fail = fail;
                    byte[] bytes = wire(output -> {
                        if (fail) {
                            assertThrows(IOException.class, () -> RecordSerializer.write(new Value("point"), output, codec));
                        } else {
                            RecordSerializer.write(new Value("point"), output, codec);
                        }
                        output.writeUTF("END");
                    });
                    assertReleased(codec.output);
                    if (previousOutput != null) {
                        assertSame(previousOutput, codec.output);
                    }
                    previousOutput = codec.output;
                    try (ObjectInput input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                        if (fail) {
                            assertThrows(IOException.class, () -> RecordSerializer.read(input, codec));
                        } else {
                            assertEquals(new Value("point"), RecordSerializer.read(input, codec));
                        }
                        assertEquals("END", input.readUTF());
                    }
                    assertReleased(codec.input);
                    if (previousInput != null) {
                        assertSame(previousInput, codec.input);
                    }
                    previousInput = codec.input;
                }
                return true;
            }).subscribeOn(scheduler).block(Duration.ofSeconds(10));
        } finally {
            scheduler.dispose();
        }
    }

    private static void assertReleased(Object frame) throws Exception {
        assertNotNull(frame);
        for (Field field : frame.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                assertTrue(field.trySetAccessible());
                assertNull("Pooled frame retains " + field.getName(), field.get(frame));
            }
        }
    }

    private static final class CapturingCodec implements SerializeUtils.Serializer {
        private ObjectOutput output;
        private ObjectInput input;
        private boolean fail;

        public int getCode() { return 0x7d; }
        public Class<?> getJavaType() { return Value.class; }

        @SneakyThrows
        public void serialize(Object value, ObjectOutput output) {
            this.output = output;
            SerializeUtils.writeObject(value, output);
            assertEquals(value, copy(value));
            SerializeUtils.writeObject(value, output);
            if (fail) {
                throw new IOException("write-failed");
            }
        }

        @SneakyThrows
        public Object deserialize(ObjectInput input) {
            this.input = input;
            Object value = SerializeUtils.readObject(input);
            assertEquals(value, copy(value));
            assertSame(value, SerializeUtils.readObject(input));
            if (fail) {
                throw new IOException("read-failed");
            }
            return value;
        }
    }
}
