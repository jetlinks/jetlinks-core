package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.types.DataTypes;
import org.jetlinks.core.metadata.types.UnknownType;

import java.util.Map;

/**
 * 可独立构造及进行 JSON 往返的事件定义，仅保存描述和注册的数据类型。
 * 实例由调用方持有和修改，不订阅或触发事件。
 *
 * @since 1.3.3
 * @see SimpleDeviceMetadata
 */
@Getter
@Setter
public class SimpleEventMetadata implements EventMetadata {

    private String id;
    private String name;
    private String description;
    private Map<String, Object> expands;
    private DataType type;

    @Override
    public DataType getType() {
        return type == null ? UnknownType.GLOBAL : type;
    }

    @Override
    public void fromJson(JSONObject json) {
        Map<String, Object> definition = MetadataDefinitionParser.parseObject(json);
        Object typeDefinition = definition.get("valueType");
        DataType parsedType = typeDefinition == null ? null
            : DataTypes.fromJsonNow(new JSONObject(MetadataDefinitionParser.parseObject(typeDefinition)));
        SimpleEventMetadata parsed = FastBeanCopier.copy(definition, SimpleEventMetadata::new, "type", "valueType");
        id = parsed.id;
        name = parsed.name;
        description = parsed.description;
        expands = parsed.expands;
        type = parsedType;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = FastBeanCopier.copy(this, JSONObject::new, "type", "valueType");
        json.put("valueType", getType().toJson());
        return json;
    }
}
