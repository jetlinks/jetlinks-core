package org.jetlinks.core.message.event;

import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.things.ThingType;

/**
 * 物事件消息
 *
 * @author zhouhao
 * @since 1.1.9
 */
public interface ThingEventMessage extends ThingMessage {

    /**
     * @return 事件标识
     * @see EventMetadata#getId()
     */
    String getEvent();

    /**
     * 事件数据，与物模型类型对应
     *
     * @return 事件数据
     * @see EventMetadata#getType()
     */
    Object getData();

    /**
     * 设置事件
     * @param event event
     * @return this
     */
    ThingEventMessage event(String event);

    /**
     * 设置事件数据
     * @param data data
     * @return this
     */
    ThingEventMessage data(Object data);

    default MessageType getMessageType() {
        return MessageType.EVENT;
    }


    static EventMessage forDevice(String deviceId) {
        EventMessage message = new EventMessage();
        message.setDeviceId(deviceId);
        return message;
    }

    static DefaultEventMessage forThing(ThingType thingType, String thingId) {
        DefaultEventMessage message = new DefaultEventMessage();
        message.setThingId(thingId);
        message.setThingType(thingType.getId());
        return message;
    }
}
