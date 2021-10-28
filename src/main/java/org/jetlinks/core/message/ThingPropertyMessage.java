package org.jetlinks.core.message;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.things.ThingProperty;

import javax.annotation.Nonnull;

/**
 * 物属性消息,用户传递物的物模型属性值
 *
 * @see org.jetlinks.core.things.ThingMetadata#getProperty(String)
 */
@Getter
@Setter
public class ThingPropertyMessage extends CommonThingMessage implements ThingProperty {

    /**
     * 属性标识
     */
    @Nonnull
    private String property;

    /**
     * 属性值
     */
    @Nonnull
    private Object value;

    /**
     * 状态,预留
     */
    private String state;

    public static ThingPropertyMessage of(@Nonnull String thingId,
                                          @Nonnull String property,
                                          @Nonnull Object value,
                                          long timestamp) {
        ThingPropertyMessage message = new ThingPropertyMessage();
        message.setThingId(thingId);
        message.setProperty(property);
        message.setTimestamp(timestamp);
        message.setValue(value);
        return message;
    }

    public ThingPropertyMessage state(String state) {
        this.state = state;
        return this;
    }

    @Override
    public final MessageType getMessageType() {
        return MessageType.THING_PROPERTY;
    }
}
