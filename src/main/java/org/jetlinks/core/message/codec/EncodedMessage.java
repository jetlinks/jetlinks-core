package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface EncodedMessage {

    @Nonnull
    ByteBuf getPayload();

    @Nonnull
    @Deprecated
    String getDeviceId();

    @Nonnull
    default MessagePayloadType getPayloadType() {
        return MessagePayloadType.JSON;
    }

    static EmptyMessage empty() {
        return EmptyMessage.INSTANCE;
    }

    static EncodedMessage simple(String deviceId, ByteBuf data) {
        return new EncodedMessage() {
            @Nonnull
            @Override
            public ByteBuf getPayload() {
                return data;
            }

            @Nonnull
            @Override
            public String getDeviceId() {
                return deviceId;
            }
        };
    }

}
