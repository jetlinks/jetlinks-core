package org.jetlinks.core.codec;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.Payload;

import javax.annotation.Nonnull;

public interface Decoder<T> {

    Class<T> forType();

    T decode(@Nonnull ByteBuf payload);

}
