package org.jetlinks.core.metadata.types;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.*;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

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
        return LocaleUtils.resolveMessage("message.metadata.type.object", LocaleUtils.current(), "对象类型");
    }

    public ObjectType addPropertyMetadata(PropertyMetadata property) {

        if (this.properties == null) {
            this.properties = new ArrayList<>();
        }

        this.properties.add(property);

        return this;
    }

    public List<PropertyMetadata> getProperties() {
        if (properties == null) {
            return Collections.emptyList();
        }
        return properties;
    }

    public ObjectType addProperty(String property, DataType type) {
        return this.addProperty(property, property, type);
    }

    public ObjectType addProperty(String property, String name, DataType type) {
        return addPropertyMetadata(SimplePropertyMetadata.of(property, name, type));
    }

    @Override
    public ValidateResult validate(Object value) {

        if (properties == null || properties.isEmpty()) {
            return ValidateResult.success(value);
        }
        Map<String, Object> mapValue = convert(value);

        for (PropertyMetadata property : properties) {
            Object data = mapValue.get(property.getId());
            if (data == null) {
                continue;
            }
            ValidateResult result = property.getValueType().validate(data);
            if (!result.isSuccess()) {
                return result;
            }
        }
        return ValidateResult.success(mapValue);
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

    public Optional<PropertyMetadata> getProperty(String key) {
        if (CollectionUtils.isEmpty(properties)) {
            return Optional.empty();
        }
        return properties
                .stream()
                .filter(prop -> prop.getId().equals(key))
                .findAny();
    }


    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        if (this.getProperties() != null) {
            json.put("properties", this
                    .getProperties()
                    .stream()
                    .map(PropertyMetadata::toJson)
                    .collect(Collectors.toList()));
        }
        return json;
    }

    @Override
    public void fromJson(JSONObject json) {
        super.fromJson(json);
        ofNullable(json.getJSONArray("properties"))
                .map(list -> list
                        .stream()
                        .map(JSON::toJSON)
                        .map(JSONObject.class::cast)
                        .<PropertyMetadata>map(j -> {
                            SimplePropertyMetadata metadata = new SimplePropertyMetadata();
                            metadata.fromJson(j);
                            return metadata;
                        })
                        .collect(Collectors.toList()))
                .ifPresent(this::setProperties);
    }
}
