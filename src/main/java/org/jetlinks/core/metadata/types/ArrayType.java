package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;

import java.util.Collection;
import java.util.stream.Collectors;

@Getter
@Setter
public class ArrayType implements DataType {

    private DataType elementType;

    public static final String ID = "array";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "数组";
    }

    @Override
    public String getDescription() {
        return "数组类型,由多个元素组成";
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
}
