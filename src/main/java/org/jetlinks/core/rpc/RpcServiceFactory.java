package org.jetlinks.core.rpc;

import reactor.core.Disposable;

/**
 * 远程服务工厂
 *
 * @author zhouhao
 * @see org.jetlinks.core.ipc.IpcService
 * @since 1.1
 */
public interface RpcServiceFactory {

    /**
     * 创建一个服务生产者,对应为远程服务,用于调用远程服务方法.
     * 注意: 接口方法参数不能为{@link org.reactivestreams.Publisher}类型.
     *
     * @param address          服务地址
     * @param serviceInterface 实现接口
     * @param <T>              接口类型
     * @return 服务
     */
    <T> DisposableService<T> createConsumer(String address, Class<T> serviceInterface);

    /**
     * 发布服务消费者,发布本地服务,远程可通过相同的地址访问此服务
     *
     * @param address          地址
     * @param serviceInterface 服务接口
     * @param instance         本地服务实现类
     * @param <T>              接口类型
     * @return Disposable
     */
    <T> Disposable createProducer(String address, Class<T> serviceInterface, T instance);

}
