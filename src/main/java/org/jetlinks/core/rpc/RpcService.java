package org.jetlinks.core.rpc;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Rpc Service 底层接口,监听rcp请求,发起rpc请求.
 *
 * @author zhouhao
 * @since 1.1
 */
public interface RpcService {

    /**
     * 监听请求,相当于发布服务.
     *
     * @param definition RPC定义
     * @param call       请求回掉
     * @param <REQ>      请求类型
     * @param <RES>      响应类型
     * @return Disposable
     */
    <REQ, RES> Disposable listen(RpcDefinition<REQ, RES> definition,
                                 BiFunction<String, REQ, Publisher<RES>> call);

    /**
     * 监听没有参数的请求.
     *
     * @param definition RPC定义
     * @param call       请求回掉
     * @param <RES>      响应类型
     * @return Disposable
     */
    <RES> Disposable listen(RpcDefinition<Void, RES> definition,
                            Function<String, Publisher<RES>> call);

    <REQ, RES> Invoker<REQ, RES> createInvoker(RpcDefinition<REQ, RES> definition);


}
