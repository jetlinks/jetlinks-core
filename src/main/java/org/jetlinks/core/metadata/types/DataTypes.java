package org.jetlinks.core.metadata.types;

import org.jetlinks.core.metadata.DataType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class DataTypes {

    private final static Map<String, Supplier<DataType>> supports = new ConcurrentHashMap<>();

    static {
        supports.put(ArrayType.ID, ArrayType::new);
        supports.put(BooleanType.ID, BooleanType::new);
        supports.put(DateTimeType.ID, DateTimeType::new);
        supports.put(DoubleType.ID, DoubleType::new);
        supports.put(EnumType.ID, EnumType::new);
        supports.put(FloatType.ID, FloatType::new);
        supports.put(IntType.ID, IntType::new);
        supports.put(LongType.ID, LongType::new);
        supports.put(ObjectType.ID, ObjectType::new);

        supports.put(StringType.ID, StringType::new);
        supports.put("text", StringType::new);

        supports.put(GeoType.ID, GeoType::new);
        supports.put(FileType.ID, FileType::new);
        supports.put(PasswordType.ID, PasswordType::new);
        supports.put(GeoShapeType.ID, GeoShapeType::new);

    }

    public static void register(String id, Supplier<DataType> supplier) {
        supports.put(id, supplier);
    }

    public static Supplier<DataType> lookup(String id) {
        return supports.get(id);
    }
}
