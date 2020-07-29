package org.jetlinks.core.rpc;

import reactor.core.Disposable;

public interface RpcServiceFactory {

    <T> DisposableService<T> createProducer(String address, Class<T> serviceInterface);

    <T> Disposable createConsumer(String address, Class<T> serviceInterface, T instance);

}
