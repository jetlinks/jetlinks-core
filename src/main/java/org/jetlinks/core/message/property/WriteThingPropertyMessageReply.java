package org.jetlinks.core.message.property;

import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.ThingMessageReply;
import org.jetlinks.core.things.ThingProperty;

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
public interface WriteThingPropertyMessageReply extends ThingMessageReply, PropertyMessage {

    /**
     * 回复的属性,key为物模型中的属性ID,value为物模型对应的类型值.
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
    WriteThingPropertyMessageReply success(Map<String, Object> properties);

    /**
     * 设置成功并设置返回完整属性值
     *
     * @param properties 属性值
     * @return this
     */
    WriteThingPropertyMessageReply success(List<ThingProperty> properties);

    default MessageType getMessageType() {
        return MessageType.READ_PROPERTY_REPLY;
    }

}
