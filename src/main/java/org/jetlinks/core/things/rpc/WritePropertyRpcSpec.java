package org.jetlinks.core.things.rpc;

import org.jetlinks.core.message.property.WriteThingPropertyMessage;
import org.jetlinks.core.message.property.WriteThingPropertyMessageReply;
import org.jetlinks.core.things.ThingRpcSupport;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 修改物属性的 RPC 规范。
 *
 * <p>在基础 {@link ThingRpcSpec} 能力上，提供便捷的属性设置方法
 * {@link #property(String, Object)} 与 {@link #properties(Map)}，
 * 用于构建 {@link WriteThingPropertyMessage}。</p>
 *
 * @param <Message> 修改属性请求消息类型
 * @param <Reply>   修改属性回复类型
 */
public interface WritePropertyRpcSpec<Message extends WriteThingPropertyMessage<Reply>, Reply
    extends WriteThingPropertyMessageReply>
    extends ThingRpcSpec<WritePropertyRpcSpec<Message, Reply>, Message, Reply> {

    /**
     * 基于同步获取的 {@link ThingRpcSupport} 与已有的修改属性消息创建规范实例。
     *
     * @param rpcSupport RPC 支持实现
     * @param baseOn     作为基础的修改属性消息（将在其上继续配置）
     * @param <Message>  请求消息类型
     * @param <Reply>    回复消息类型
     * @return WritePropertyRpcSpec 实例
     */
    static <Message extends WriteThingPropertyMessage<Reply>, Reply
        extends WriteThingPropertyMessageReply> WritePropertyRpcSpec<Message, Reply> create(ThingRpcSupport rpcSupport,
                                                                                           Message baseOn) {
        return create(Mono.just(rpcSupport), baseOn);
    }

    /**
     * 基于异步获取的 {@link ThingRpcSupport} 与已有的修改属性消息创建规范实例。
     *
     * @param rpcSupport 以 Mono 包装的 RPC 支持实现
     * @param baseOn     作为基础的修改属性消息（将在其上继续配置）
     * @param <Message>  请求消息类型
     * @param <Reply>    回复消息类型
     * @return WritePropertyRpcSpec 实例
     */
    static <Message extends WriteThingPropertyMessage<Reply>, Reply
        extends WriteThingPropertyMessageReply> WritePropertyRpcSpec<Message, Reply> create(Mono<ThingRpcSupport> rpcSupport,
                                                                                           Message baseOn) {
        return new DefaultWritePropertyRpcSpec<>(rpcSupport, baseOn);
    }

    /**
     * 设置单个属性值。
     *
     * @param property 属性ID
     * @param value    属性值
     * @return this
     */
    default WritePropertyRpcSpec<Message, Reply> property(String property, Object value) {
        return custom(msg -> msg.addProperty(property, value));
    }

    /**
     * 批量设置属性值。
     *
     * @param properties 属性键值对
     * @return this
     */
    default WritePropertyRpcSpec<Message, Reply> properties(Map<String, Object> properties) {
        return custom(msg -> properties.forEach(msg::addProperty));
    }

    @Override
    Mono<Reply> execute();

    @Override
    Reply executeBlocking();
}


