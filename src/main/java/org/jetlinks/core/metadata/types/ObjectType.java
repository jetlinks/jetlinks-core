package org.jetlinks.core.metadata.types;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Getter
@Setter
@Slf4j
public class ObjectType extends AbstractType<ObjectType> implements DataType, Converter<Map<String, Object>> {
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

    public ObjectType addPropertyMetadata(PropertyMetadata property) {

        if (this.properties == null) {
            this.properties = new ArrayList<>();
        }

        this.properties.add(property);

        return this;
    }

    public ObjectType addProperty(String property, DataType type) {
        return this.addProperty(property, property, type);
    }

    public ObjectType addProperty(String property, String name, DataType type) {
        SimplePropertyMetadata metadata = new SimplePropertyMetadata();
        metadata.setId(property);
        metadata.setName(name);
        metadata.setValueType(type);
        return addPropertyMetadata(metadata);
    }

    @Override
    public ValidateResult validate(Object value) {

        if (properties == null || properties.isEmpty()) {
            return ValidateResult.success();
        }
        Map<String, Object> mapValue = convert(value);

        for (PropertyMetadata property : properties) {
            Object data = mapValue.get(property.getId());
            ValidateResult result = property.getValueType().validate(data);
            if (!result.isSuccess()) {
                return result;
            }
        }
        return  ValidateResult.success(mapValue);
    }

    @Override
    public JSONObject format(Object value) {
        return new JSONObject(handle(value, DataType::format));
    }

    @SuppressWarnings("all")
    public Map<String, Object> handle(Object value, BiFunction<DataType, Object, Object> mapping) {
        if (value == null) {
            return null;
        }
        if (value instanceof String && ((String) value).startsWith("{")) {
            value = JSON.parseObject(String.valueOf(value));
        }
        if (!(value instanceof Map)) {
            value = FastBeanCopier.copy(value, new HashMap<>());
        }
        if (value instanceof Map) {
            Map<String, Object> mapValue = new HashMap<>(((Map) value));
            if (properties != null) {
                for (PropertyMetadata property : properties) {
                    Object data = mapValue.get(property.getId());
                    DataType type = property.getValueType();
                    if (data != null) {
                        mapValue.put(property.getId(), mapping.apply(type, data));
                    }
                }
            }
            return mapValue;
        }
        return null;
    }

    @Override
    public Map<String, Object> convert(Object value) {
        return handle(value, (type, data) -> {
            if (type instanceof Converter) {
                return ((Converter<?>) type).convert(data);
            }
            return data;
        });
    }
}
