package org.jetlinks.core.message.property;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.metadata.types.LongType;
import org.jetlinks.core.things.ThingProperty;
import org.jetlinks.core.utils.MapUtils;

import java.util.List;
import java.util.Map;

/**
 * 上报设备属性,通常由设备定时上报,方向: 设备->平台
 *
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class ReportPropertyMessage extends CommonDeviceMessage<ReportPropertyMessage> implements ThingReportPropertyMessage {

    /**
     * 属性值信息,key为物模型中的属性ID,value为物模型对应的类型值.
     * <p>
     * 注意: value如果是结构体(对象类型),请勿传入在协议包中自定义的对象,应该转为{@link Map}传入.
     */
    private Map<String, Object> properties;

    /**
     * 属性源的时间戳,表示不同属性值产生的时间戳,单位毫秒
     *
     * @since 1.1.7
     */
    private Map<String, Long> propertySourceTimes;

    /**
     * 属性状态信息
     *
     * @since 1.1.7
     */
    private Map<String,String> propertyStates;

    public static ReportPropertyMessage create() {
        return new ReportPropertyMessage();
    }

    public ReportPropertyMessage success(Map<String, Object> properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public ReportPropertyMessage propertySourceTimes(Map<String, Long> times) {
        this.propertySourceTimes = times;
        return this;
    }

    @Override
    public ReportPropertyMessage propertyStates(Map<String, String> states) {
        this.propertyStates = states;
        return this;
    }

    @Override
    public ReportPropertyMessage properties(Map<String, Object> properties) {
        return success(properties);
    }

    @Override
    public ReportPropertyMessage success(List<ThingProperty> properties) {
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
        return MessageType.REPORT_PROPERTY;
    }

}
