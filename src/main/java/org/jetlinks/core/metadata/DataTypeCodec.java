package org.jetlinks.core.metadata;

import java.util.Map;

public interface DataTypeCodec<T extends DataType> {

    String getTypeId();

    T decode(T type, Map<String,Object> config);

    Map<String,Object> encode(T type);

}
