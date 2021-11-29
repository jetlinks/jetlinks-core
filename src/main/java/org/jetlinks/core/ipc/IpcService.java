package org.jetlinks.core.ipc;

import reactor.core.Disposable;

/**
 * Ipc Service 底层接口,监听IPC请求,发起IPC请求.
 * 通常用于进程间通信
 *
 * @author zhouhao
 * @since 1.1.5
 */
@Deprecated
public interface IpcService {

    /**
     * 监听请求,相当于发布服务.
     *
     * @param definition IPC定义
     * @param processor  执行器
     * @param <REQ>      请求类型
     * @param <RES>      响应类型
     * @return Disposable
     */
    <REQ, RES> Disposable listen(IpcDefinition<REQ, RES> definition,
                                 IpcInvoker<REQ, RES> processor);

    /**
     * 创建调用器,用于调用服务
     *
     * @param definition IPC定义
     * @param <REQ>      请求类型
     * @param <RES>      响应类型
     * @return 服务调用器
     */
    <REQ, RES> IpcInvoker<REQ, RES> createInvoker(String name, IpcDefinition<REQ, RES> definition);


}
