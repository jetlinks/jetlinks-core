package org.jetlinks.core.codec;

import org.jetlinks.core.Payload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Decoder<T> {

    @Nullable
    T decode(@Nonnull Payload payload);

}
