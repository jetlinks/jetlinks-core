package org.jetlinks.core.rpc;

import lombok.AllArgsConstructor;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class LocalRpcManager implements RpcManager {

    private final Map<String, RpcServiceInfo<?>> services = new ConcurrentHashMap<>();

    @Override
    public String currentServerId() {
        return "localhost";
    }

    @Override
    public <T> Disposable registerService(T rpcService) {
        return registerService(rpcService.getClass().getName(), rpcService);
    }

    @Override
    public <T> Disposable registerService(String serviceId, T rpcService) {
        RpcServiceInfo<T> serviceInfo = new RpcServiceInfo<>(
                serviceId,
                rpcService.getClass().getSimpleName(),
                currentServerId(),
                rpcService);
        services.put(serviceId, serviceInfo);
        return () -> services.remove(serviceId, serviceInfo);
    }

    @Override
    public <I> Flux<RpcService<I>> getServices(Class<I> service) {
        return Flux.fromIterable(services.values())
                   .mapNotNull(e -> {
                       if (service.isInstance(e.service)) {
                           return ((RpcService<I>) e);
                       }
                       return null;
                   });
    }

    @Override
    public <I> Mono<RpcService<I>> selectService(Class<I> service) {
        return Flux.fromIterable(services.values())
                   .mapNotNull(e -> {
                       if (service.isInstance(e.service)) {
                           return ((RpcService<I>) e);
                       }
                       return null;
                   })
                   .take(1)
                   .singleOrEmpty();
    }

    @Override
    public <I> Flux<RpcService<I>> getServices(String serviceId, Class<I> service) {
        return Flux.fromIterable(services.values())
                   .mapNotNull(e -> {
                       if (Objects.equals(e.id, serviceId) && service.isInstance(e.service)) {
                           return ((RpcService<I>) e);
                       }
                       return null;
                   });
    }

    @Override
    public <I> Mono<I> getService(String serverNodeId, Class<I> service) {
        return selectService(service)
                .map(RpcService::service);
    }

    @Override
    public <I> Mono<I> getService(String serverNodeId, String serviceId, Class<I> service) {
        return getServices(serviceId, service)
                .take(1)
                .singleOrEmpty()
                .map(RpcService::service);
    }

    @Override
    public <I> Flux<ServiceEvent> listen(Class<I> service) {
        return Flux.empty();
    }

    @Override
    public Flux<RpcService<?>> getServices() {
        return Flux.fromIterable(services.values());
    }

    @AllArgsConstructor
    static class RpcServiceInfo<T> implements RpcService<T> {
        private final String id;
        private final String name;
        private final String serverNodeId;

        private final T service;

        @Override
        public String serverNodeId() {
            return serverNodeId;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public T service() {
            return service;
        }
    }
}
