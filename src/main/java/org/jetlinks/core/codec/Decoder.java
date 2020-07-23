package org.jetlinks.core.codec;

import org.jetlinks.core.Payload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Decoder<T> {

    Class<T> forType();

    @Nullable
    T decode(@Nonnull Payload payload);

    default boolean isDecodeFrom(Object nativeObject){
        if(nativeObject==null){
            return false;
        }
        return forType().isInstance(nativeObject);
    }
}
