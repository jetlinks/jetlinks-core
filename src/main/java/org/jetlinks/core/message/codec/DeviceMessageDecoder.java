package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.Message;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;

/**
 * 设备消息解码器，用于将收到设备上传的消息解码为可读的消息。
 *
 * @author zhouhao
 * @since 1.0
 */
public interface DeviceMessageDecoder {

    /**
     * 在服务器收到设备或者网络组件中发来的消息时，会调用协议包中的此方法来进行解码，
     * 将数据{@link EncodedMessage}转为平台的统一消息{@link org.jetlinks.core.message.DeviceMessage}
     *
     * <pre>{@code
     * //解码并返回一个消息
     * public Mono<DeviceMessage> decode(MessageDecodeContext context){
     *
     *  EncodedMessage message = context.getMessage();
     *  byte[] payload = message.payloadAsBytes();//上报的数据
     *
     *  DeviceMessage message = doEncode(payload);
     *
     *  return Mono.just(message);
     * }
     *
     * //解码并返回多个消息
     * public Flux<DeviceMessage> decode(MessageDecodeContext context){
     *
     *  EncodedMessage message = context.getMessage();
     *  byte[] payload = message.payloadAsBytes();//上报的数据
     *
     *  List<DeviceMessage> messages = doEncode(payload);
     *
     *  return Flux.fromIterable(messages);
     * }
     *
     * //解码,回复设备并返回一个消息
     * public Mono<DeviceMessage> decode(MessageDecodeContext context){
     *
     *  EncodedMessage message = context.getMessage();
     *  byte[] payload = message.payloadAsBytes();//上报的数据
     *
     *  DeviceMessage message = doEncode(payload); //解码
     *
     *  FromDeviceMessageContext ctx = (FromDeviceMessageContext)context;
     *
     *  EncodedMessage msg = createReplyMessage(); //构造回复
     *
     *  return ctx
     *     .getSession()
     *     .send(msg) //发送回复
     *     .thenReturn(message);
     * }
     *
     * }</pre>
     * <p>
     * ⚠️注意：返回子设备的消息需要统一使用{@link org.jetlinks.core.message.ChildDeviceMessage}或者{@link org.jetlinks.core.message.ChildDeviceMessageReply}
     * 包装,否则设备会话可能创建错误导致下发指令与预期不符.
     *
     * @param context 消息上下文
     * @return 解码结果
     * @see MqttMessage
     * @see org.jetlinks.core.message.codec.http.HttpExchangeMessage
     * @see CoapExchangeMessage
     * @see org.jetlinks.core.message.DeviceMessageReply
     * @see org.jetlinks.core.message.property.ReadPropertyMessageReply
     * @see org.jetlinks.core.message.function.FunctionInvokeMessageReply
     * @see org.jetlinks.core.message.ChildDeviceMessageReply
     * @see org.jetlinks.core.message.DeviceOnlineMessage
     * @see org.jetlinks.core.message.DeviceOfflineMessage
     * @see org.jetlinks.core.message.interceptor.DeviceMessageDecodeInterceptor
     * @see org.jetlinks.core.message.DeviceDataManager
     */
    @Nonnull
    Publisher<? extends Message> decode(@Nonnull MessageDecodeContext context);
}
