package org.jetlinks.core.device;

import org.jetlinks.core.message.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 * 消息发送器,用于发送消息给设备.
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMessageSender {

    /**
     * 发送一个支持回复的消息.
     * <p>
     * ⚠️: 请勿自己实现消息对象,而应该使用框架定义的3种消息.
     * ⚠️: 如果是异步消息,将直接返回<code>{"success":true,"code":"REQUEST_HANDLING"}</code>
     *
     * @param message 具体的消息对象
     * @param <R>     返回类型
     * @return 异步发送结果
     * @see org.jetlinks.core.message.property.ReadPropertyMessage
     * @see org.jetlinks.core.message.property.ReadPropertyMessageReply
     * @see org.jetlinks.core.message.property.WritePropertyMessage
     * @see org.jetlinks.core.message.property.WritePropertyMessageReply
     * @see org.jetlinks.core.message.function.FunctionInvokeMessage
     * @see org.jetlinks.core.message.function.FunctionInvokeMessageReply
     * @see org.jetlinks.core.enums.ErrorCode#CLIENT_OFFLINE
     * @see org.jetlinks.core.enums.ErrorCode#REQUEST_HANDLING
     * @see org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor
     */
    <R extends DeviceMessageReply> Flux<R> send(Publisher<RepayableDeviceMessage<R>> message);

    /**
     * 发送消息并自定义返回结果转换器
     *
     * @param message      消息
     * @param replyMapping 消息回复转换器
     * @param <R>          回复类型
     * @return 异步发送结果
     * @see this#send(Publisher)
     */
    <R extends DeviceMessage> Flux<R> send(Publisher<? extends DeviceMessage> message, Function<Object, R> replyMapping);

    /**
     * 发送{@link org.jetlinks.core.message.function.FunctionInvokeMessage}消息更便捷的API
     *
     * @param function 要执行的功能
     * @return FunctionInvokeMessageSender
     * @see this#send(Publisher)
     * @see org.jetlinks.core.message.function.FunctionInvokeMessage
     * @see FunctionInvokeMessageSender
     */
    FunctionInvokeMessageSender invokeFunction(String function);

    /**
     * 发送{@link org.jetlinks.core.message.property.ReadPropertyMessage}消息更便捷的API
     *
     * @param property 要获取的属性列表
     * @return ReadPropertyMessageSender
     * @see this#send(Publisher)
     * @see org.jetlinks.core.message.property.ReadPropertyMessage
     * @see ReadPropertyMessageSender
     */
    ReadPropertyMessageSender readProperty(String... property);

    /**
     * 发送{@link org.jetlinks.core.message.property.WritePropertyMessage}消息更便捷的API
     *
     * @return WritePropertyMessageSender
     * @see this#send(Publisher)
     * @see org.jetlinks.core.message.property.WritePropertyMessage
     * @see WritePropertyMessageSender
     */
    WritePropertyMessageSender writeProperty();

}
