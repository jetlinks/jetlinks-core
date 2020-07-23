package org.jetlinks.core;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

@Getter
@Setter
@NoArgsConstructor
public class NativePayload implements Payload {

    private Object nativeObject;

    private Supplier<ByteBuf> bodySupplier;

    private volatile ByteBuf ref;

    public static NativePayload of(Object nativeObject, Supplier<ByteBuf> bodySupplier) {
        NativePayload payload = new NativePayload();

        payload.nativeObject = nativeObject;
        payload.bodySupplier = bodySupplier;
        return payload;
    }

    @Nonnull
    @Override
    public ByteBuf getBody() {
        return ref == null ? ref = bodySupplier.get() : ref;
    }

    @Override
    public <T> T bodyAs(Class<T> type) {
        if (type.isInstance(nativeObject)) {
            return type.cast(nativeObject);
        }
        return Payload.super.bodyAs(type);
    }

    @Override
    public String toString() {
        return nativeObject == null ? "null" : nativeObject.toString();
    }
}
