package org.jetlinks.core.message.property;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.metadata.types.LongType;
import org.jetlinks.core.things.ThingProperty;
import org.jetlinks.core.utils.MapUtils;

import java.util.List;
import java.util.Map;

/**
 * 读取设备属性消息回复, 方向: 设备->平台
 * <p>
 * 在设备接收到{@link ReadPropertyMessage}消息后,使用此消息进行回复,回复后,指令发起方将收到响应结果.
 *
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class ReadPropertyMessageReply extends CommonDeviceMessageReply<ReadPropertyMessageReply> implements ReadThingPropertyMessageReply {

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
    private Map<String, String> propertyStates;

    public static ReadPropertyMessageReply create() {
        ReadPropertyMessageReply reply = new ReadPropertyMessageReply();

        reply.setTimestamp(System.currentTimeMillis());

        return reply;
    }

    public ReadPropertyMessageReply success(Map<String, Object> properties) {

        this.properties = properties;
        super.setSuccess(true);
        return this;

    }

    @Override
    public ReadPropertyMessageReply success(List<ThingProperty> properties) {
        this.properties = Maps.newLinkedHashMapWithExpectedSize(properties.size());
        this.propertySourceTimes = Maps.newLinkedHashMapWithExpectedSize(properties.size());
        this.propertyStates = Maps.newLinkedHashMapWithExpectedSize(properties.size());
        for (ThingProperty property : properties) {
            this.properties.put(property.getProperty(), property.getValue());
            this.propertySourceTimes.put(property.getProperty(), property.getTimestamp());
            this.propertyStates.put(property.getProperty(), property.getState());
        }
        return this;
    }

    @Override
    public ReadPropertyMessageReply propertySourceTimes(Map<String, Long> times) {
        this.propertySourceTimes = times;
        return this;
    }

    @Override
    public ReadPropertyMessageReply propertyStates(Map<String, String> states) {
        this.propertyStates = states;
        return this;
    }

    @Override
    public ReadPropertyMessageReply properties(Map<String, Object> properties) {
        this.properties = properties;
        return this;
    }


    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        this.properties = jsonObject.getJSONObject("properties");
        this.propertySourceTimes =
                MapUtils.convertKeyValue(
                        jsonObject.getJSONObject("propertySourceTimes"),
                        String::valueOf,
                        LongType.GLOBAL::convert
                );
        this.propertyStates = MapUtils
                .convertKeyValue(
                        jsonObject.getJSONObject("propertyStates"),
                        String::valueOf,
                        String::valueOf
                );
    }

    public MessageType getMessageType() {
        return MessageType.READ_PROPERTY_REPLY;
    }

}
