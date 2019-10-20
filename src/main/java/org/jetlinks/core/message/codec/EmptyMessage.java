package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public final class EmptyMessage implements EncodedMessage {

    public static final EmptyMessage INSTANCE = new EmptyMessage();

    private EmptyMessage() {
    }

    @Nonnull
    @Override
    public ByteBuf getPayload() {
        throw new UnsupportedOperationException("无法从空消息中获取ByteBuf");
    }

    @Nonnull
    @Override
    public String getDeviceId() {
        throw new UnsupportedOperationException();
    }
}
