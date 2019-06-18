package org.jetlinks.core.device;

import org.jetlinks.core.message.*;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 消息发送器,用于发送消息给设备.
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMessageSender {

    /**
     * 尝试获取设备已经回复,但是未及时获取对设备返回对消息. 如果设备未回复或者回复的消息已经超时
     * 将会得到结果: {"success":false,"code":"NO_REPLY"}
     *
     * @param messageId 消息ID
     * @param replyNewInstance 回复对象提供者
     * @param <R> 回复类型
     * @return 异步获取结果
     * @see org.jetlinks.core.enums.ErrorCode#NO_REPLY
     */
    <R extends DeviceMessageReply> CompletionStage<R> retrieveReply(String messageId, Supplier<R> replyNewInstance);

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
    <R extends DeviceMessageReply> CompletionStage<R> send(RepayableDeviceMessage<R> message);

    /**
     * 发送消息并自定义返回结果转换器
     *
     * @param message      消息
     * @param replyMapping 消息回复转换器
     * @param <R>          回复类型
     * @return 异步发送结果
     * @see this#send(RepayableDeviceMessage)
     */
    <R extends DeviceMessageReply> CompletionStage<R> send(DeviceMessage message, Function<Object, R> replyMapping);

    /**
     * 发送{@link org.jetlinks.core.message.function.FunctionInvokeMessage}消息更便捷的API
     *
     * @param function 要执行的功能
     * @return FunctionInvokeMessageSender
     * @see this#send(RepayableDeviceMessage)
     * @see org.jetlinks.core.message.function.FunctionInvokeMessage
     * @see FunctionInvokeMessageSender
     */
    FunctionInvokeMessageSender invokeFunction(String function);

    /**
     * 发送{@link org.jetlinks.core.message.property.ReadPropertyMessage}消息更便捷的API
     *
     * @param property 要获取的属性列表
     * @return ReadPropertyMessageSender
     * @see this#send(RepayableDeviceMessage)
     * @see org.jetlinks.core.message.property.ReadPropertyMessage
     * @see ReadPropertyMessageSender
     */
    ReadPropertyMessageSender readProperty(String... property);

    /**
     * 发送{@link org.jetlinks.core.message.property.WritePropertyMessage}消息更便捷的API
     *
     * @return WritePropertyMessageSender
     * @see this#send(RepayableDeviceMessage)
     * @see org.jetlinks.core.message.property.WritePropertyMessage
     * @see WritePropertyMessageSender
     */
    WritePropertyMessageSender writeProperty();

}
