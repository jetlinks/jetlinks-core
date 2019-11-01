package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ArrayType implements DataType, Converter<List<Object>> {

    private DataType elementType;

    public static final String ID = "array";

    private String description;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "数组";
    }

    @Override
    public ValidateResult validate(Object value) {
        if (elementType != null && value instanceof Collection) {
            Collection collection = ((Collection) value);
            for (Object data : collection) {
                ValidateResult result = elementType.validate(data);
                if (!result.isSuccess()) {
                    return result;
                }
            }
        }
        return ValidateResult.success();
    }

    @Override
    public Object format(Object value) {

        if (elementType != null && value instanceof Collection) {
            Collection<Object> collection = ((Collection) value);
            return collection.stream()
                    .map(data -> elementType.format(data))
                    .collect(Collectors.toList());
        }

        return value;
    }

    @Override
    public List<Object> convert(Object value) {
        if (value instanceof Collection) {
            return ((Collection<Object>) value).stream()
                    .map(val -> {
                        if (elementType instanceof Converter) {
                            return ((Converter) elementType).convert(val);
                        }
                        return val;
                    }).collect(Collectors.toList());
        }
        return Collections.singletonList(value);
    }
}
