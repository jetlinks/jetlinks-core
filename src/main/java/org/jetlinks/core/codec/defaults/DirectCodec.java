package org.jetlinks.core.codec.defaults;

import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;


public class DirectCodec implements Codec<Payload> {

    public static final DirectCodec INSTANCE = new DirectCodec();

    @Override
    public Class<Payload> forType() {
        return Payload.class;
    }

    @Override
    public Payload decode(@Nonnull Payload payload) {
        return payload;
    }

    @Override
    public Payload encode(Payload body) {
        return body;
    }
}
