package org.jetlinks.core.message.property;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;

import java.util.Map;
import java.util.Optional;

/**
 * 上报设备属性,通常由设备定时上报,方向: 设备->平台
 *
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class ReportPropertyMessage extends CommonDeviceMessage {

    /**
     * 属性,key为物模型中的属性ID,value为物模型对应的类型值.
     * <p>
     * 注意: value如果是结构体(对象类型),请勿传入在协议包中自定义的对象,应该转为{@link Map}传入.
     */
    private Map<String, Object> properties;

    public static ReportPropertyMessage create() {
        return new ReportPropertyMessage();
    }

    public ReportPropertyMessage success(Map<String, Object> properties) {
        this.properties = properties;
        return this;
    }

    public Optional<Object> getProperty(String property) {
        return Optional
                .ofNullable(properties)
                .map(map -> map.get(property));
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        this.properties = jsonObject.getJSONObject("properties");
    }

    public MessageType getMessageType() {
        return MessageType.REPORT_PROPERTY;
    }

}
