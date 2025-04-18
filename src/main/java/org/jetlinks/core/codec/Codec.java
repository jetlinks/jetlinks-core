package org.jetlinks.core.codec;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public interface Codec<T> {

    Class<T> forType();

    T decode(@Nonnull ByteBuf payload,Endian endian);

    void encode(T body, ByteBuf buf,Endian endian);

    enum Endian{
        Big,Little
    }
}
