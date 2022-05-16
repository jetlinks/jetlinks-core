package org.jetlinks.core.ipc;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.codec.Codec;

@AllArgsConstructor
@Getter
class DefaultIpcDefinition<REQ, RES> implements IpcDefinition<REQ, RES> {
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

    @Override
    public String toString() {
        return "IPC:" + address  + "";
    }
}
