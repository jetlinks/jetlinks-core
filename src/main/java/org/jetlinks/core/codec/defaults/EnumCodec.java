package org.jetlinks.core.codec.defaults;

import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.Payload;

import javax.annotation.Nonnull;
import java.util.Arrays;

@AllArgsConstructor(staticName = "of")
public class EnumCodec<T extends Enum<?>> implements Codec<T> {

    private final T[] values;

    @Override
    @SuppressWarnings("all")
    public Class<T> forType() {
        return (Class<T>) values[0].getDeclaringClass();
    }

    @Override
    public T decode(@Nonnull Payload payload) {
        byte[] bytes = payload.getBytes();

        if (bytes.length > 0 && bytes[0] <= values.length) {
            return values[bytes[0] & 0xFF];
        }
        throw new IllegalArgumentException("can not decode payload " + Arrays.toString(bytes) + " to enums " + Arrays.toString(values));
    }

    @Override
    public Payload encode(T body) {
        return () -> Unpooled.wrappedBuffer(new byte[]{(byte) body.ordinal()});
    }


}
