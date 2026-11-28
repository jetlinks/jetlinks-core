package org.jetlinks.core.utils;

import lombok.SneakyThrows;

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
    private static final ThreadLocal<Scope> SCOPES = ThreadLocal.withInitial(() -> new Scope(null));

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
        Scope owner = enter(output);
        Scope scope = SCOPES.get();
        try {
            if (scope.writtenRecords == null) {
                scope.writtenRecords = new IdentityHashMap<>();
            }
            Integer previous = scope.writtenRecords.get(value);
            if (previous != null) {
                if (previous < 0) {
                    throw new InvalidObjectException("Cyclic record reference: " + value.getClass().getName());
                }
                output.writeInt(-previous);
                return;
            }
            int identity = scope.writtenRecords.size() + 1;
            scope.writtenRecords.put(value, -identity);
            RecordMetadata metadata = RecordMetadata.of(value.getClass());
            output.writeInt(identity);
            if (scope.writtenSchemas == null) {
                scope.writtenSchemas = new HashMap<>();
            }
            Integer schemaId = scope.writtenSchemas.get(value.getClass());
            if (schemaId == null) {
                schemaId = scope.writtenSchemas.size() + 1;
                scope.writtenSchemas.put(value.getClass(), schemaId);
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
            scope.writtenRecords.put(value, identity);
        } finally {
            if (owner != null) {
                owner.close();
            }
        }
    }

    @Override
    @SneakyThrows
    public Object deserialize(ObjectInput input) {
        Scope owner = enter(input);
        Scope scope = SCOPES.get();
        try {
            if (scope.readRecords == null) {
                scope.readRecords = new ArrayList<>();
            }
            int identity = input.readInt();
            if (identity < 0) {
                long index = -(long) identity - 1;
                if (index >= scope.readRecords.size() || scope.readRecords.get((int) index) == null) {
                    throw new StreamCorruptedException("Unknown or cyclic record reference: " + identity);
                }
                return scope.readRecords.get((int) index);
            }
            if (identity != scope.readRecords.size() + 1) {
                throw new StreamCorruptedException("Invalid record identity: " + identity);
            }
            ReadSchema schema = readSchema(input, scope);
            scope.readRecords.add(null);
            Object[] values = schema.metadata.arguments();
            for (int index : schema.argumentIndexes) {
                Object value = SerializeUtils.readObject(input);
                if (index >= 0) {
                    values[index] = value;
                }
            }
            Object value = schema.metadata.newInstance(values);
            scope.readRecords.set(identity - 1, value);
            return value;
        } finally {
            if (owner != null) {
                owner.close();
            }
        }
    }

    private static ReadSchema readSchema(ObjectInput input, Scope scope) throws IOException, ClassNotFoundException {
        if (scope.readSchemas == null) {
            scope.readSchemas = new ArrayList<>();
        }
        int schemaId = input.readInt();
        if (schemaId > 0) {
            if (schemaId > scope.readSchemas.size()) {
                throw new StreamCorruptedException("Unknown record schema: " + schemaId);
            }
            return scope.readSchemas.get(schemaId - 1);
        }
        if (-(long) schemaId != scope.readSchemas.size() + 1) {
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
        scope.readSchemas.add(schema);
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

    /** 同一流复用当前帧并返回null，只有非null的owner负责结束和恢复父帧。 */
    static Scope enter(Object stream) {
        Scope scope = SCOPES.get();
        if (scope.stream != null && scope.stream == stream) {
            return null;
        }
        if (scope.stream != null) {
            scope = new Scope(scope);
            SCOPES.set(scope);
        }
        scope.stream = stream;
        return scope;
    }

    static final class Scope {
        private final Scope parent;
        private Object stream;
        private IdentityHashMap<Object, Integer> writtenRecords;
        private ArrayList<Object> readRecords;
        private HashMap<Class<?>, Integer> writtenSchemas;
        private ArrayList<ReadSchema> readSchemas;

        private Scope(Scope parent) {
            this.parent = parent;
        }

        void close() {
            // 仅外层owner清理；同流递归调用不重复计数或分配。
            stream = null;
            writtenRecords = null;
            readRecords = null;
            writtenSchemas = null;
            readSchemas = null;
            if (parent != null) {
                SCOPES.set(parent);
            }
        }
    }
}
