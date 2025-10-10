package org.jetlinks.core.things.rpc;

import org.jetlinks.core.message.property.ReadThingPropertyMessage;
import org.jetlinks.core.message.property.ReadThingPropertyMessageReply;
import org.jetlinks.core.things.ThingRpcSupport;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * 读取物属性的 RPC 规范。
 *
 * <p>在基础 {@link ThingRpcSpec} 能力上，提供便捷的属性选择方法
 * {@link #properties(String...)} 与 {@link #properties(List)}，
 * 用于构建 {@link ReadThingPropertyMessage}。</p>
 *
 * @param <Message> 读取属性请求消息类型
 * @param <Reply>   读取属性回复类型
 */
public interface ReadPropertyRpcSpec<Message extends ReadThingPropertyMessage<Reply>, Reply
    extends ReadThingPropertyMessageReply>
    extends ThingRpcSpec<ReadPropertyRpcSpec<Message, Reply>, Message, Reply> {

    /**
     * 基于同步获取的 {@link ThingRpcSupport} 与已有的读取属性消息创建规范实例。
     *
     * @param rpcSupport RPC 支持实现
     * @param baseOn     作为基础的读取属性消息（将在其上继续配置）
     * @param <Message>  请求消息类型
     * @param <Reply>    回复消息类型
     * @return ReadPropertyRpcSpec 实例
     */
    static <Message extends ReadThingPropertyMessage<Reply>, Reply
        extends ReadThingPropertyMessageReply> ReadPropertyRpcSpec<Message, Reply> create(ThingRpcSupport rpcSupport,
                                                                                          Message baseOn) {
        return create(Mono.just(rpcSupport), baseOn);
    }

    /**
     * 基于异步获取的 {@link ThingRpcSupport} 与已有的读取属性消息创建规范实例。
     *
     * @param rpcSupport 以 Mono 包装的 RPC 支持实现
     * @param baseOn     作为基础的读取属性消息（将在其上继续配置）
     * @param <Message>  请求消息类型
     * @param <Reply>    回复消息类型
     * @return ReadPropertyRpcSpec 实例
     */
    static <Message extends ReadThingPropertyMessage<Reply>, Reply
        extends ReadThingPropertyMessageReply> ReadPropertyRpcSpec<Message, Reply> create(Mono<ThingRpcSupport> rpcSupport,
                                                                                          Message baseOn) {
        return new DefaultReadPropertyRpcSpec<>(rpcSupport, baseOn);
    }

    default ReadPropertyRpcSpec<Message, Reply> properties(String... properties) {
        return properties(Arrays.asList(properties));
    }

    default ReadPropertyRpcSpec<Message, Reply> properties(List<String> properties) {
        return custom(msg -> msg.addProperties(properties));
    }

    @Override
    Mono<Reply> execute();

    @Override
    Reply executeBlocking();


}
