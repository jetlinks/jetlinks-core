package org.jetlinks.core.rpc;

import reactor.core.Disposable;

public interface DisposableService<S> extends Disposable {

    S getService();

}
