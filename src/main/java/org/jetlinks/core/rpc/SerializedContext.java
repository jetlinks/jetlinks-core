package org.jetlinks.core.rpc;

import reactor.core.Disposable;


public interface SerializedContext extends Disposable {


    static SerializedContext empty() {
        return EmptySerializedContext.INSTANCE;
    }

    String getContext();

    @Override
    default void dispose() {
    }
}
