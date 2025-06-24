package org.jetlinks.core.message.property;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.things.ThingProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class WritePropertyMessageReply extends CommonDeviceMessageReply<WritePropertyMessageReply> implements WriteThingPropertyMessageReply {

    /**
     * 回复的属性,key为物模型中的属性ID,value为物模型对应的类型值.
     * <p>
     * 注意: value如果是结构体(对象类型),请勿传入在协议包中自定义的对象,应该转为{@link Map}传入.
     */
    @Schema(title = "回复的属性", description = "key为物模型中的属性ID,value为物模型对应的类型值")
    private Map<String, Object> properties;

    /**
     * 属性源的时间戳,表示不同属性值产生的时间戳,单位毫秒
     *
     * @since 1.1.7
     */
    @Schema(title = "属性源的时间戳", description = "表示不同属性值产生的时间戳,单位毫秒")
    private Map<String, Long> propertySourceTimes;

    /**
     * 属性状态信息
     *
     * @since 1.1.7
     */
    @Schema(title = "属性状态信息")
    private Map<String,String> propertyStates;

    public WritePropertyMessageReply success(Map<String, Object> properties) {

        this.properties = properties;
        super.setSuccess(true);
        return this;

    }

    @Override
    public WritePropertyMessageReply success(List<ThingProperty> properties) {
        this.properties = new LinkedHashMap<>();
        this.propertySourceTimes = new LinkedHashMap<>();
        this.propertyStates = new LinkedHashMap<>();
        for (ThingProperty property : properties) {
            this.properties.put(property.getProperty(), property.getValue());
            this.propertySourceTimes.put(property.getProperty(), property.getTimestamp());
            this.propertyStates.put(property.getProperty(), property.getState());
        }
        return this;
    }

    @Override
    public WritePropertyMessageReply propertySourceTimes(Map<String, Long> times) {
        this.propertySourceTimes = times;
        return this;
    }

    @Override
    public WritePropertyMessageReply propertyStates(Map<String, String> states) {
        this.propertyStates = states;
        return this;
    }

    @Override
    public WritePropertyMessageReply properties(Map<String, Object> properties) {
        return success(properties);
    }

    public synchronized WritePropertyMessageReply addProperty(String key, Object value) {
        if (properties == null) {
            properties = new LinkedHashMap<>();
        }
        properties.put(key, value);
        return this;
    }


    public static WritePropertyMessageReply create() {
        WritePropertyMessageReply reply = new WritePropertyMessageReply();

        reply.setTimestamp(System.currentTimeMillis());

        return reply;
    }

    @Override
    @SuppressWarnings("all")
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        this.properties = jsonObject.getJSONObject("properties");
        this.propertySourceTimes = (Map) jsonObject.getJSONObject("propertySourceTimes");
        this.propertyStates = (Map) jsonObject.getJSONObject("propertyStates");
    }

    public MessageType getMessageType() {
        return MessageType.WRITE_PROPERTY_REPLY;
    }

}
