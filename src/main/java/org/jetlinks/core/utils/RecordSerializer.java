package org.jetlinks.core.utils;

import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import org.hswebframework.web.recycler.Recycler;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 按组件名编码record。0x24为独立类型码，旧reader必须升级后才能接收此格式。
 * 单次SerializeUtils调用内保留record共享引用，循环回指明确拒绝。
 */
final class RecordSerializer implements SerializeUtils.Serializer {
    static final int CODE = 0x24;
    static final RecordSerializer INSTANCE = new RecordSerializer();
    private static final int MAX_CACHED_FRAMES = 256;
    private static final Recycler<OutputFrame> OUTPUTS =
        Recycler.create(OutputFrame::new, OutputFrame::reset, MAX_CACHED_FRAMES);
    private static final Recycler<InputFrame> INPUTS =
        Recycler.create(InputFrame::new, InputFrame::reset, MAX_CACHED_FRAMES);

    static void write(Object value, ObjectOutput output, SerializeUtils.Serializer serializer) {
        OUTPUTS.doWith(value, output, serializer, (frame, object, target, codec) -> {
            frame.output = target;
            codec.serialize(object, frame);
            return null;
        });
    }

    static Object read(ObjectInput input, SerializeUtils.Serializer serializer) {
        return INPUTS.doWith(input, serializer, (frame, source, codec) -> {
            frame.input = source;
            return codec.deserialize(frame);
        });
    }

    @Override
    public int getCode() {
        return CODE;
    }

    @Override
    public Class<?> getJavaType() {
        return Record.class;
    }

    @Override
    @SneakyThrows
    public void serialize(Object value, ObjectOutput output) {
        if (!(output instanceof OutputFrame frame)) {
            write(value, output, this);
            return;
        }
        if (frame.writtenRecords == null) {
            frame.writtenRecords = new IdentityHashMap<>();
        }
        Integer previous = frame.writtenRecords.get(value);
        if (previous != null) {
            if (previous < 0) {
                throw new InvalidObjectException("Cyclic record reference: " + value.getClass().getName());
            }
            output.writeInt(-previous);
            return;
        }
        int identity = frame.writtenRecords.size() + 1;
        frame.writtenRecords.put(value, -identity);
        RecordMetadata metadata = RecordMetadata.of(value.getClass());
        output.writeInt(identity);
        if (frame.writtenSchemas == null) {
            frame.writtenSchemas = new HashMap<>();
        }
        Integer schemaId = frame.writtenSchemas.get(value.getClass());
        if (schemaId == null) {
            schemaId = frame.writtenSchemas.size() + 1;
            frame.writtenSchemas.put(value.getClass(), schemaId);
            output.writeInt(-schemaId);
            output.writeUTF(value.getClass().getName());
            output.writeInt(metadata.size());
            for (int index = 0; index < metadata.size(); index++) {
                output.writeUTF(metadata.name(index));
            }
        } else {
            output.writeInt(schemaId);
        }
        for (int index = 0; index < metadata.size(); index++) {
            SerializeUtils.writeObject(metadata.value(value, index), output);
        }
        frame.writtenRecords.put(value, identity);
    }

    @Override
    @SneakyThrows
    public Object deserialize(ObjectInput input) {
        if (!(input instanceof InputFrame frame)) {
            return read(input, this);
        }
        if (frame.readRecords == null) {
            frame.readRecords = new ArrayList<>();
        }
        int identity = input.readInt();
        if (identity < 0) {
            long index = -(long) identity - 1;
            if (index >= frame.readRecords.size() || frame.readRecords.get((int) index) == null) {
                throw new StreamCorruptedException("Unknown or cyclic record reference: " + identity);
            }
            return frame.readRecords.get((int) index);
        }
        if (identity != frame.readRecords.size() + 1) {
            throw new StreamCorruptedException("Invalid record identity: " + identity);
        }
        ReadSchema schema = readSchema(frame);
        frame.readRecords.add(null);
        Object[] values = schema.metadata.arguments();
        for (int index : schema.argumentIndexes) {
            Object value = SerializeUtils.readObject(input);
            if (index >= 0) {
                values[index] = value;
            }
        }
        Object value = schema.metadata.newInstance(values);
        frame.readRecords.set(identity - 1, value);
        return value;
    }

    private static ReadSchema readSchema(InputFrame input) throws IOException, ClassNotFoundException {
        if (input.readSchemas == null) {
            input.readSchemas = new ArrayList<>();
        }
        int schemaId = input.readInt();
        if (schemaId > 0) {
            if (schemaId > input.readSchemas.size()) {
                throw new StreamCorruptedException("Unknown record schema: " + schemaId);
            }
            return input.readSchemas.get(schemaId - 1);
        }
        if (-(long) schemaId != input.readSchemas.size() + 1) {
            throw new StreamCorruptedException("Invalid record schema: " + schemaId);
        }
        String typeName = input.readUTF();
        Class<?> type = SerializeUtils.getClass(typeName);
        if (!type.isRecord()) {
            throw new InvalidObjectException("Not a record: " + typeName);
        }
        RecordMetadata metadata = RecordMetadata.of(type);
        int count = input.readInt();
        if (count < 0) {
            throw new StreamCorruptedException("Negative record component count: " + count);
        }
        // 按实际读入的名称增长，同时拒绝未知字段的重复声明。
        Map<String, Integer> components = new LinkedHashMap<>(Math.min(count, metadata.size()));
        for (int field = 0; field < count; field++) {
            String name = input.readUTF();
            Integer index = metadata.index(name);
            if (components.putIfAbsent(name, index == null ? -1 : index) != null) {
                throw new StreamCorruptedException("Duplicate record component: " + name);
            }
        }
        int[] argumentIndexes = new int[components.size()];
        int position = 0;
        for (int index : components.values()) {
            argumentIndexes[position++] = index;
        }
        ReadSchema schema = new ReadSchema(metadata, argumentIndexes);
        input.readSchemas.add(schema);
        return schema;
    }

    private static final class ReadSchema {
        final RecordMetadata metadata;
        final int[] argumentIndexes;

        ReadSchema(RecordMetadata metadata, int[] argumentIndexes) {
            this.metadata = metadata;
            this.argumentIndexes = argumentIndexes;
        }
    }

    static final class OutputFrame implements ObjectOutput {
        @Delegate
        private ObjectOutput output;
        private IdentityHashMap<Object, Integer> writtenRecords;
        private HashMap<Class<?>, Integer> writtenSchemas;

        void reset() {
            // 仅复用空包装器，帧集合与峰值容量不留在池中。
            output = null;
            writtenRecords = null;
            writtenSchemas = null;
        }
    }

    static final class InputFrame implements ObjectInput {
        @Delegate
        private ObjectInput input;
        private ArrayList<Object> readRecords;
        private ArrayList<ReadSchema> readSchemas;

        void reset() {
            input = null;
            readRecords = null;
            readSchemas = null;
        }
    }
}
