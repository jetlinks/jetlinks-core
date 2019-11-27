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
    default MessagePayloadType getPayloadType() {
        return MessagePayloadType.JSON;
    }

    static EmptyMessage empty() {
        return EmptyMessage.INSTANCE;
    }

    static EncodedMessage simple(ByteBuf data) {
        return () -> data;
    }

}
