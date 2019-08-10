package org.jetlinks.core.support;

import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DataTypeCodec;
import org.jetlinks.core.support.types.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JetLinksDataTypeCodecs {

    private static final Map<String, DataTypeCodec<? extends DataType>> codecMap = new HashMap<>();

    {
        register(new JetLinksArrayCodec());
        register(new JetLinksBooleanCodec());
        register(new JetLinksDateCodec());
        register(new JetLinksDoubleCodec());
        register(new JetLinksEnumCodec());
        register(new JetLinksFloatCodec());
        register(new JetLinksIntCodec());
        register(new JetLinksLongCodec());
        register(new JetLinksObjectCodec());
        register(new JetLinksStringCodec());

    }

    public static void register(DataTypeCodec<? extends DataType> codec) {
        codecMap.put(codec.getTypeId(), codec);
    }

    public static Optional< DataTypeCodec<DataType>> getCodec(String typeId) {

        return Optional.ofNullable((DataTypeCodec) codecMap.get(typeId));
    }
}
