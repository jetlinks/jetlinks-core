package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface EncodedMessage {

    @Nonnull
    ByteBuf getByteBuf();

    @Nonnull
    String getDeviceId();

    static EmptyMessage empty() {
        return EmptyMessage.INSTANCE;
    }

    static EncodedMessage simple(String deviceId, ByteBuf data) {
        return new EncodedMessage() {
            @Nonnull
            @Override
            public ByteBuf getByteBuf() {
                return data;
            }

            @Nonnull
            @Override
            public String getDeviceId() {
                return deviceId;
            }
        };
    }

    /**
     * 构造一个mqtt消息
     *
     * @param deviceId 设备ID
     * @param topic    mqtt topic
     * @param data     数据内容
     * @return MqttMessage
     */
    static MqttMessage mqtt(@Nonnull String deviceId, @Nonnull String topic, @Nonnull ByteBuf data) {
        return new MqttMessage() {
            @Override
            @Nonnull
            public String getTopic() {
                return topic;
            }

            @Override
            @Nonnull
            public String getDeviceId() {
                return deviceId;
            }

            @Override
            @Nonnull
            public ByteBuf getByteBuf() {
                return data;
            }
        };
    }
}
