package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttProperties;
import org.apache.commons.codec.binary.Hex;
import org.jetlinks.core.utils.CharsetUtils;
import org.jetlinks.core.utils.StringBuilderUtils;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface MqttMessage extends EncodedMessage {

    /**
     * 构造MQTT消息,使用qos 0.
     *
     * @param topic   topic
     * @param payload payload
     * @return message
     */
    static MqttMessage create(String topic, ByteBuf payload) {
        return builder()
            .topic(topic)
            .payload(payload)
            .build();
    }

    /**
     * 构造MQTT消息,使用自定义qos.
     *
     * @param topic   topic
     * @param payload payload
     * @return message
     */
    static MqttMessage create(String topic, ByteBuf payload, int qos) {
        return builder()
            .topic(topic)
            .payload(payload)
            .qosLevel(qos)
            .build();
    }

    static SimpleMqttMessage.SimpleMqttMessageBuilder builder() {
        return SimpleMqttMessage.builder();
    }

    /**
     * 获取Topic
     *
     * @return topic
     */
    @Nonnull
    String getTopic();

    /**
     * 获取clientId
     *
     * @return clientId
     */
    String getClientId();


    int getMessageId();

    default boolean isWill() {
        return false;
    }

    /**
     * 获取QoS
     *
     * @return qos
     */
    default int getQosLevel() {
        return 0;
    }

    default boolean isDup() {
        return false;
    }

    default boolean isRetain() {
        return false;
    }

    /**
     * 获取MQTT 5 指定的自定义属性
     *
     * @return 自定义属性
     * @see io.netty.handler.codec.mqtt.MqttProperties
     */
    default MqttProperties getProperties() {
        return MqttProperties.NO_PROPERTIES;
    }

    default String print() {
        return StringBuilderUtils
            .buildString(this, (msg, builder) -> {
                builder.append("qos").append(getQosLevel()).append(" ").append(getTopic()).append("\n")
                       .append("messageId: ").append(getMessageId()).append("\n")
                       .append("dup: ").append(isDup()).append("\n")
                       .append("retain: ").append(isRetain()).append("\n")
                       .append("will: ").append(isWill()).append("\n\n");
                ByteBuf payload = getPayload();
                if (payload.isReadable()) {
                    String text = payload.toString(StandardCharsets.UTF_8);
                    if (CharsetUtils.isHumanFriendly(text)) {
                        builder.append(text);
                    } else {
                        ByteBufUtil.appendPrettyHexDump(builder, payload);
                    }
                } else {
                    builder.append("<released>");
                }

            });
    }

}
