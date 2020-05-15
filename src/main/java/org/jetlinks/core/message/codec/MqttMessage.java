package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface MqttMessage extends EncodedMessage {

    @Nonnull
    String getTopic();

    String getClientId();

    int getMessageId();

    default boolean isWill() {
        return false;
    }

    default int getQosLevel() {
        return 0;
    }

    default boolean isDup() {
        return false;
    }

    default boolean isRetain() {
        return false;
    }

    default String print() {
        StringBuilder builder = new StringBuilder();
        builder.append("qos").append(getQosLevel()).append(" ").append(getTopic()).append("\n")
                .append("messageId: ").append(getMessageId()).append("\n")
                .append("dup: ").append(isDup()).append("\n")
                .append("retain: ").append(isRetain()).append("\n")
                .append("will: ").append(isWill()).append("\n\n");

        ByteBuf payload = getPayload();
        if (ByteBufUtil.isText(payload, StandardCharsets.UTF_8)) {
            builder.append(payload.toString(StandardCharsets.UTF_8));
        } else {
            ByteBufUtil.appendPrettyHexDump(builder, payload);
        }
        return builder.toString();
    }
}
