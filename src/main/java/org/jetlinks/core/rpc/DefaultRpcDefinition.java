package org.jetlinks.core.rpc;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.codec.Codec;

@AllArgsConstructor
@Getter
public class DefaultRpcDefinition<REQ, RES> implements RpcDefinition<REQ, RES> {
    private final String id;
    private final String address;

    private final Codec<REQ> requestCodec;
    private final Codec<RES> responseCodec;


    @Override
    public Codec<REQ> requestCodec() {
        return requestCodec;
    }

    @Override
    public Codec<RES> responseCodec() {
        return responseCodec;
    }
}
