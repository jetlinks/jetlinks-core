package org.jetlinks.core.codec;

import org.jetlinks.core.buffer.Buffer;

import javax.annotation.Nonnull;
import java.nio.ByteOrder;

public interface Codec<T> {

    Class<T> forType();

    T decode(@Nonnull Buffer payload, ByteOrder endian);

    void encode(T body, Buffer buf,ByteOrder endian);

}
