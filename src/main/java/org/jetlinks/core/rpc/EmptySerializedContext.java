package org.jetlinks.core.rpc;

import java.io.Serializable;

class EmptySerializedContext implements SerializedContext{
    static final EmptySerializedContext INSTANCE = new EmptySerializedContext();

    @Override
    public String getContext() {
        return null;
    }

    @Override
    public void dispose() {

    }
}
