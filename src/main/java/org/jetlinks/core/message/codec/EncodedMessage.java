package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface EncodedMessage {

    @Nonnull
    ByteBuf getPayload();

    default byte[] getBytes() {
        return ByteBufUtil.getBytes(getPayload());
    }

    default byte[] getBytes(int offset, int len) {
        return ByteBufUtil.getBytes(getPayload(), offset, len);
    }

    @Nullable
    default MessagePayloadType getPayloadType() {
        return MessagePayloadType.JSON;
    }

    static EmptyMessage empty() {
        return EmptyMessage.INSTANCE;
    }

    static EncodedMessage simple(ByteBuf data) {
        return simple(data, MessagePayloadType.BINARY);
    }

    static EncodedMessage simple(ByteBuf data, MessagePayloadType payloadType) {
        return SimpleEncodedMessage.of(data, payloadType);
    }

}
