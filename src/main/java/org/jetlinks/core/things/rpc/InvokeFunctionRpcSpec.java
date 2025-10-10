package org.jetlinks.core.things.rpc;

import org.jetlinks.core.message.function.ThingFunctionInvokeMessage;
import org.jetlinks.core.message.function.ThingFunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadThingPropertyMessage;
import org.jetlinks.core.message.property.ReadThingPropertyMessageReply;
import org.jetlinks.core.things.ThingRpcSupport;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 调用物功能的 RPC 规范。
 *
 * <p>在基础 {@link ThingRpcSpec} 能力上，提供便捷的功能入参设置方法
 * {@link #input(String, Object)} 与 {@link #inputs(Map)}，
 * 用于构建 {@link ThingFunctionInvokeMessage}。</p>
 *
 * @param <Message> 功能调用请求消息类型
 * @param <Reply>   功能调用回复类型
 */
public interface InvokeFunctionRpcSpec<Message extends ThingFunctionInvokeMessage<Reply>, Reply
    extends ThingFunctionInvokeMessageReply>
    extends ThingRpcSpec<InvokeFunctionRpcSpec<Message, Reply>, Message, Reply> {


    /**
     * 基于同步获取的 {@link ThingRpcSupport} 与已有的功能调用消息创建规范实例。
     *
     * @param rpcSupport RPC 支持实现
     * @param baseOn     作为基础的功能调用消息（将在其上继续配置）
     * @param <Message>  请求消息类型
     * @param <Reply>    回复消息类型
     * @return InvokeFunctionRpcSpec 实例
     */
    static <Message extends ThingFunctionInvokeMessage<Reply>, Reply
        extends ThingFunctionInvokeMessageReply> InvokeFunctionRpcSpec<Message, Reply> create(ThingRpcSupport rpcSupport,
                                                                                              Message baseOn) {
        return create(Mono.just(rpcSupport), baseOn);
    }

    /**
     * 基于异步获取的 {@link ThingRpcSupport} 与已有的功能调用消息创建规范实例。
     *
     * @param rpcSupport 以 Mono 包装的 RPC 支持实现
     * @param baseOn     作为基础的功能调用消息（将在其上继续配置）
     * @param <Message>  请求消息类型
     * @param <Reply>    回复消息类型
     * @return InvokeFunctionRpcSpec 实例
     */
    static <Message extends ThingFunctionInvokeMessage<Reply>, Reply
        extends ThingFunctionInvokeMessageReply> InvokeFunctionRpcSpec<Message, Reply> create(Mono<ThingRpcSupport> rpcSupport,
                                                                                              Message baseOn) {
        return new DefaultFunctionInvokeRpcSpec<>(rpcSupport, baseOn);
    }

    default InvokeFunctionRpcSpec<Message, Reply> input(String arg, Object value) {
        return custom(msg -> msg.addInput(arg, value));
    }

    default InvokeFunctionRpcSpec<Message, Reply> inputs(Map<String, Object> maps) {
        return custom(msg -> msg.addInputs(maps));
    }

    @Override
    Mono<Reply> execute();

    @Override
    Reply executeBlocking();

}
