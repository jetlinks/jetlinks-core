package org.jetlinks.core.message;

import org.jetlinks.core.Routable;
import org.jetlinks.core.things.ThingId;
import org.jetlinks.core.things.ThingType;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 物消息
 *
 * @since 1.1.9
 */
public interface ThingMessage extends Message, Routable {

    /**
     * @return 物类型
     */
    String getThingType();

    /**
     * @return 物ID
     */
    String getThingId();

    /**
     * 设置messageId
     *
     * @param messageId messageId
     * @return ThingMessage
     */
    ThingMessage messageId(String messageId);

    /**
     * 设置物类型和ID
     *
     * @param thingType 物类型
     * @param thingId   物ID
     * @return this
     */
    ThingMessage thingId(String thingType, String thingId);

    /**
     * 设置物消息时间戳
     *
     * @param timestamp 时间戳
     * @return this
     */
    ThingMessage timestamp(long timestamp);

    /**
     * 设置物类型和ID
     *
     * @param thingType 物类型
     * @param thingId   物ID
     * @return this
     */
    default ThingMessage thingId(ThingType thingType, String thingId) {
        return thingId(thingType.getId(), thingId);
    }

    /**
     * 设置物类型和ID
     *
     * @param thingId 物ID
     * @return this
     */
    default ThingMessage thingId(ThingId thingId) {
        return thingId(thingId.getType(), thingId.getId());
    }

    @Override
    default ThingMessage copy() {
        return (ThingMessage) Message.super.copy();
    }

    @Override
    default void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        SerializeUtils.readKeyValue(in, this::addHeader);
        thingId(SerializeUtils.readNullableUTF(in), SerializeUtils.readNullableUTF(in));
        messageId(SerializeUtils.readNullableUTF(in));
        timestamp(in.readLong());
    }

    @Override
    default void writeExternal(ObjectOutput out) throws IOException {
        //header
        SerializeUtils.writeKeyValue(getHeaders(), out);

        SerializeUtils.writeNullableUTF(getThingType(), out);
        SerializeUtils.writeNullableUTF(getThingId(), out);
        SerializeUtils.writeNullableUTF(getMessageId(), out);
        out.writeLong(getTimestamp());
    }

    @Override
    default Object routeKey() {
        return getHeaderOrElse(
            Headers.routeKey,
            this::getThingId);
    }
}
