package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.ValidateResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ObjectType implements DataType {
    public static final String ID = "object";

    private List<PropertyMetadata> properties;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "对象类型";
    }

    @Override
    public String getDescription() {
        return "复杂结构对象类型";
    }

    @Override
    public ValidateResult validate(Object value) {

        if (properties == null || properties.isEmpty()) {
            return ValidateResult.success();
        }
        if (value instanceof Map) {
            Map<String, Object> mapValue = ((Map) value);
            for (PropertyMetadata property : properties) {
                Object data = mapValue.get(property.getId());
                ValidateResult result = property.getValueType().validate(data);
                if (!result.isSuccess()) {
                    return result;
                }
            }
        }

        return ValidateResult.fail("不支持的格式");
    }

    @Override
    public Object format(Object value) {
        if (properties != null && value instanceof Map) {
            Map<String, Object> mapValue = new HashMap<>(((Map) value));
            for (PropertyMetadata property : properties) {
                Object data = mapValue.get(property.getId());
                if (data != null) {
                    mapValue.put(property.getId(), property.getValueType().format(data));
                }
            }
            return mapValue;
        }
        return value;
    }
}
