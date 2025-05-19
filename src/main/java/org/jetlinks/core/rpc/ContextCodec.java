package org.jetlinks.core.rpc;

import reactor.util.context.Context;
import reactor.util.context.ContextView;


public interface ContextCodec {

    ContextCodec DEFAULT = new ContextCodec() {
    };


    default SerializedContext serialize(ContextView context) {
        return SerializedContext.empty();
    }

    default Context deserialize(Context source, SerializedContext ctx) {
        return Context.empty();
    }


}
