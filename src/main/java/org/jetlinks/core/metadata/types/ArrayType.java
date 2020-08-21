package org.jetlinks.core.metadata.types;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
public class ArrayType extends AbstractType<ArrayType> implements DataType, Converter<List<Object>> {

    public static final String ID = "array";

    private DataType elementType;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "数组";
    }

    public ArrayType elementType(DataType elementType) {
        this.elementType = elementType;
        return this;
    }

    @Override
    public ValidateResult validate(Object value) {

        List<Object> listValue = convert(value);
        if (elementType != null && value instanceof Collection) {
            for (Object data : listValue) {
                ValidateResult result = elementType.validate(data);
                if (!result.isSuccess()) {
                    return result;
                }
            }
        }
        return ValidateResult.success(listValue);
    }

    @Override
    public Object format(Object value) {

        if (elementType != null && value instanceof Collection) {
            Collection<?> collection = ((Collection<?>) value);
            return new JSONArray(collection.stream()
                    .map(data -> elementType.format(data))
                    .collect(Collectors.toList()));
        }

        return JSON.toJSON(value);
    }

    @Override
    public List<Object> convert(Object value) {
        if (value instanceof Collection) {
            return ((Collection<?>) value).stream()
                    .map(val -> {
                        if (elementType instanceof Converter) {
                            return ((Converter<?>) elementType).convert(val);
                        }
                        return val;
                    }).collect(Collectors.toList());
        }
        return Collections.singletonList(value);
    }
}
