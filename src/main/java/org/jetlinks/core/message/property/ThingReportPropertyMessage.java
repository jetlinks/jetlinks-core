package org.jetlinks.core.message.property;

import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.things.ThingProperty;
import org.jetlinks.core.things.ThingType;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.Map;

/**
 * 上报设备属性,通常由设备定时上报,方向: 设备->平台
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface ThingReportPropertyMessage extends ThingMessage, PropertyMessage {

    /**
     * 属性值信息,key为物模型中的属性ID,value为物模型对应的类型值.
     * <p>
     * 注意: value如果是结构体(对象类型),请勿传入在协议包中自定义的对象,应该转为{@link Map}传入.
     */
    Map<String, Object> getProperties();

    /**
     * 属性源的时间戳,表示不同属性值产生的时间戳,单位毫秒
     *
     * @since 1.1.7
     */
    Map<String, Long> getPropertySourceTimes();

    /**
     * 属性状态信息
     *
     * @since 1.1.7
     */
    Map<String, String> getPropertyStates();

    /**
     * 设置成功并设置返回属性值
     *
     * @param properties 属性值
     * @return this
     */
    ThingReportPropertyMessage success(Map<String, Object> properties);

    /**
     * 设置成功并设置返回完整属性值
     *
     * @param properties 属性值
     * @return this
     */
    ThingReportPropertyMessage success(List<ThingProperty> properties);

    default MessageType getMessageType() {
        return MessageType.REPORT_PROPERTY;
    }

    static ReportPropertyMessage forDevice(String deviceId) {
        ReportPropertyMessage message = new ReportPropertyMessage();
        message.setDeviceId(deviceId);
        return message;
    }

    static DefaultReportPropertyMessage forThing(ThingType thingType, String deviceId) {
        DefaultReportPropertyMessage message = new DefaultReportPropertyMessage();
        message.setThingId(deviceId);
        message.setThingType(thingType.getId());
        return message;
    }

    @Override
    default void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ThingMessage.super.readExternal(in);
        PropertyMessage.super.readExternal(in);
    }

    @Override
    default void writeExternal(ObjectOutput out) throws IOException {
        ThingMessage.super.writeExternal(out);
        PropertyMessage.super.writeExternal(out);
    }
}
