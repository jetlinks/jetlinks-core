package org.jetlinks.core.codec;

import org.jetlinks.core.buffer.Buffer;

import javax.annotation.Nonnull;

public interface Codec<T> {

    Class<T> forType();

    T decode(@Nonnull Buffer payload);

    void encode(T body, Buffer buf);

}
