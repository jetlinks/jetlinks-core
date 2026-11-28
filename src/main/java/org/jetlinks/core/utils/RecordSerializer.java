package org.jetlinks.core.utils;

import lombok.SneakyThrows;

import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.HashMap;

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
            if (scope.written == null) {
                scope.written = new IdentityHashMap<>();
            }
            Integer previous = scope.written.get(value);
            if (previous != null) {
                if (previous < 0) {
                    throw new InvalidObjectException("Cyclic record reference: " + value.getClass().getName());
                }
                output.writeInt(-previous);
                return;
            }
            int identity = scope.written.size() + 1;
            scope.written.put(value, -identity);
            RecordMetadata metadata = RecordMetadata.of(value.getClass());
            output.writeInt(identity);
            if (scope.writtenTypes == null) {
                scope.writtenTypes = new HashMap<>();
            }
            Integer typeId = scope.writtenTypes.get(value.getClass());
            if (typeId == null) {
                typeId = scope.writtenTypes.size() + 1;
                scope.writtenTypes.put(value.getClass(), typeId);
                output.writeInt(-typeId);
                output.writeUTF(value.getClass().getName());
                output.writeInt(metadata.size());
                for (int index = 0; index < metadata.size(); index++) {
                    output.writeUTF(metadata.name(index));
                }
            } else {
                output.writeInt(typeId);
            }
            for (int index = 0; index < metadata.size(); index++) {
                SerializeUtils.writeObject(metadata.value(value, index), output);
            }
            scope.written.put(value, identity);
        } finally {
            if (owner != null) owner.close();
        }
    }

    @Override
    @SneakyThrows
    public Object deserialize(ObjectInput input) {
        Scope owner = enter(input);
        Scope scope = SCOPES.get();
        try {
            if (scope.read == null) {
                scope.read = new ArrayList<>();
            }
            int identity = input.readInt();
            if (identity < 0) {
                long index = -(long) identity - 1;
                if (index >= scope.read.size() || scope.read.get((int) index) == null) {
                    throw new StreamCorruptedException("Unknown or cyclic record reference: " + identity);
                }
                return scope.read.get((int) index);
            }
            if (identity != scope.read.size() + 1) {
                throw new StreamCorruptedException("Invalid record identity: " + identity);
            }
            Schema schema = readSchema(input, scope);
            scope.read.add(null);
            Object[] values = schema.metadata.arguments();
            for (int index : schema.indexes) {
                Object value = SerializeUtils.readObject(input);
                if (index >= 0) {
                    values[index] = value;
                }
            }
            Object value = schema.metadata.newInstance(values);
            scope.read.set(identity - 1, value);
            return value;
        } finally {
            if (owner != null) owner.close();
        }
    }

    private static Schema readSchema(ObjectInput input, Scope scope) throws java.io.IOException, ClassNotFoundException {
        if (scope.readTypes == null) {
            scope.readTypes = new ArrayList<>();
        }
        int typeId = input.readInt();
        if (typeId > 0) {
            if (typeId > scope.readTypes.size()) {
                throw new StreamCorruptedException("Unknown record schema: " + typeId);
            }
            return scope.readTypes.get(typeId - 1);
        }
        if (-(long) typeId != scope.readTypes.size() + 1) {
            throw new StreamCorruptedException("Invalid record schema: " + typeId);
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
        // Do not allocate from an untrusted count before consuming the actual field names.
        ArrayList<Integer> slots = new ArrayList<>(Math.min(count, metadata.size()));
        boolean[] assigned = new boolean[metadata.size()];
        for (int field = 0; field < count; field++) {
            String name = input.readUTF();
            Integer index = metadata.index(name);
            if (index != null) {
                if (assigned[index]) {
                    throw new StreamCorruptedException("Duplicate record component: " + name);
                }
                assigned[index] = true;
            }
            slots.add(index == null ? -1 : index);
        }
        int[] indexes = new int[slots.size()];
        for (int index = 0; index < indexes.length; index++) {
            indexes[index] = slots.get(index);
        }
        Schema schema = new Schema(metadata, indexes);
        scope.readTypes.add(schema);
        return schema;
    }

    private static final class Schema {
        final RecordMetadata metadata;
        final int[] indexes;

        Schema(RecordMetadata metadata, int[] indexes) {
            this.metadata = metadata;
            this.indexes = indexes;
        }
    }

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
        private IdentityHashMap<Object, Integer> written;
        private ArrayList<Object> read;
        private HashMap<Class<?>, Integer> writtenTypes;
        private ArrayList<Schema> readTypes;

        private Scope(Scope parent) {
            this.parent = parent;
        }

        void close() {
            // 仅外层owner清理；同流递归调用不重复计数或分配。
            stream = null;
            written = null;
            read = null;
            writtenTypes = null;
            readTypes = null;
            if (parent != null) {
                SCOPES.set(parent);
            }
        }
    }
}
