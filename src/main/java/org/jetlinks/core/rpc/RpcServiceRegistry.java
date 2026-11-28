package org.jetlinks.core.rpc;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.stream.Collector;

/**
 * RPC 服务的注册、查询、选择与监听；返回的服务句柄负责执行。
 *
 * @author zhouhao
 * @since 1.2
 */
public interface RpcServiceRegistry {

    /**
     * 注册RPC服务实现类,可调用返回值{@link  Disposable#dispose()}来注销服务
     *
     * @param rpcService 服务
     * @param <T>        服务实现类
     * @return dispose
     */
    <T> Disposable registerService(T rpcService);

    /**
     * 注册指定id标识的RPC服务实现类,可调用返回值{@link  Disposable#dispose()}来注销服务
     *
     * @param rpcService 服务
     * @param <T>        服务实现类
     * @return dispose
     */
    <T> Disposable registerService(String serviceId, T rpcService);

    /**
     * 获取全部指定接口的服务
     *
     * @param service 服务接口类
     * @param <I>     接口类型
     * @return RPC接口
     */
    <I> Flux<RpcService<I>> getServices(Class<I> service);

    /**
     * 选择一个服务
     *
     * @param service 服务类型
     * @param <I>     服务类型
     * @return 选择结果
     */
    <I> Mono<RpcService<I>> selectService(Class<I> service);

    /**
     * 选择一个服务
     *
     * @param service 服务类型
     * @param <I>     服务类型
     * @return 选择结果
     */
    <T,I> Mono<I> selectService(Class<I> service,
                                Collector<RpcService<I>, T, Optional<RpcService<I>>> selector,
                                Mono<I> fallback);

    /**
     * 根据路由key选择一个服务,相同key会选择相同的服务.
     *
     * @param service  服务类型
     * @param routeKey 路由key
     * @param <I>      服务类型
     * @return 选择结果
     */
    default <I> Mono<RpcService<I>> selectService(Class<I> service, Object routeKey) {
        return selectService(service);
    }

    /**
     * 获取指定服务ID的RPC服务接口
     *
     * @param serviceId 服务ID
     * @param service   RPC接口
     * @param <I>       服务接口类
     * @return RPC接口
     */
    <I> Flux<RpcService<I>> getServices(String serviceId, Class<I> service);

    /**
     * 获取指定节点ID的RPC服务接口,用于进行点对点调用
     *
     * @param serverNodeId 集群节点ID
     * @param service      RPC接口
     * @param <I>          服务接口类
     * @return RPC接口
     */
    <I> Mono<I> getService(String serverNodeId,
                           Class<I> service);

    /**
     * 获取指定节点ID的指定服务ID的RPC服务接口,用于进行点对点调用
     *
     * @param serverNodeId 集群节点ID
     * @param service      RPC接口
     * @param serviceId    服务ID
     * @param <I>          服务接口类
     * @return RPC接口
     */
    <I> Mono<I> getService(String serverNodeId,
                           String serviceId,
                           Class<I> service);


    /**
     * 监听服务注册，注销事件
     *
     * @param service 服务接口类
     * @param <I>     服务接口类型
     * @return 事件流
     */
    <I> Flux<ServiceEvent> listen(Class<I> service);

    /**
     * 获取全部服务元数据。实现可返回未绑定调用接口的条目；调用请使用带 Class 的查询。
     *
     * @return 全部服务
     */
    Flux<RpcService<?>> getServices();

    /**
     * 原子交付初始完整服务视图及后续更新，不支持该能力的实现显式失败。
     */
    default <I> Flux<RpcServiceView<I>> watchServices(Class<I> service) {
        return Flux.error(new UnsupportedOperationException("Atomic service views are not supported"));
    }

}
