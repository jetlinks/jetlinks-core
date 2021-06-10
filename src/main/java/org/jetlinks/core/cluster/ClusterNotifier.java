package org.jetlinks.core.cluster;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 集群通知器,用于向集群节点发送通知
 *
 * @author zhouhao
 * @since 1.0
 * @see org.jetlinks.core.event.EventBus
 */
@Deprecated
public interface ClusterNotifier {

    /**
     * 发送通知给指定的服务节点
     *
     * @param serverNodeId 服务节点ID
     * @param address      消息地址
     * @param payload      消息内容
     * @return 发送结果
     */
    Mono<Boolean> sendNotify(String serverNodeId, String address, Publisher<?> payload);

    /**
     * 发送通知给指定的服务节点并等待返回,目标服务必须使用{@link ClusterNotifier#handleNotify(String, Function)}进行监听
     *
     * @param serverNodeId 服务节点
     * @param address      消息地址
     * @param payload      消息内容
     * @param <T>          返回值结果类型
     * @return 返回结果
     */
    <T> Flux<T> sendNotifyAndReceive(String serverNodeId, String address, Publisher<?> payload);

    /**
     * 调用此方法开始处理通知,当收到对应消息地址上的消息时,消息会进入Flux下游
     *
     * @param address 消息地址
     * @param <T>     消息类型
     * @return 通知消息流
     */
    <T> Flux<T> handleNotify(String address);

    /**
     * 调用此方法开始处理通知并回复处理结果
     *
     * @param address      消息地址
     * @param replyHandler 处理器
     * @param <T>          消息类型
     * @param <R>          处理结果类型
     */
    <T, R> Mono<Void> handleNotify(String address, Function<T, Publisher<R>> replyHandler);

}
