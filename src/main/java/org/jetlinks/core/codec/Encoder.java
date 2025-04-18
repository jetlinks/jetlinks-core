package org.jetlinks.core.codec;

import io.netty.buffer.ByteBuf;

public interface Encoder<T> {

    Class<T> forType();

    void encode(T body, ByteBuf buf);

}
