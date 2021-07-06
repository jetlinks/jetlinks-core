package org.jetlinks.core.message.codec;

import org.jetlinks.core.utils.ParallelIntervalHelper;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;

/**
 * 设备消息编码器,用于将消息对象编码为对应消息协议的消息
 *
 * @see EncodedMessage
 * @see org.jetlinks.core.message.Message
 */
public interface DeviceMessageEncoder {

    /**
     * 编码,将消息进行编码,用于发送到设备端.
     *
     * 平台在发送指令给设备时,会调用协议包中设置的此方法,将平台消息{@link org.jetlinks.core.message.DeviceMessage}转为设备能理解的消息{@link EncodedMessage}.
     *
     * 例如:
     * <pre>
     *
     * //返回单个消息给设备,多个使用Flux&lt;EncodedMessage&gt;作为返回值
     * public Mono&lt;EncodedMessage&gt; encode(MessageEncodeContext context){
     *
     *     return Mono.just(doEncode(context.getMessage()));
     *
     * }
     *</pre>
     *
     * <pre>
     * //忽略发送给设备,直接返回结果给指令发送者
     * public Mono&lt;EncodedMessage&gt; encode(MessageEncodeContext context){
     *    DeviceMessage message = (DeviceMessage)context.getMessage();
     *
     *    return context
     *      .reply(handleMessage(message)) //返回结果给指令发送者
     *      .then(Mono.empty())
     * }
     *
     * </pre>
     *
     * 如果要串行发送数据,可以参考使用{@link ParallelIntervalHelper}工具类
     * @param context 消息上下文
     * @return 编码结果
     * @see MqttMessage
     * @see org.jetlinks.core.message.Message
     * @see org.jetlinks.core.message.property.ReadPropertyMessage 读取设备属性
     * @see org.jetlinks.core.message.property.WritePropertyMessage 修改设备属性
     * @see org.jetlinks.core.message.function.FunctionInvokeMessage 调用设备功能
     * @see org.jetlinks.core.message.ChildDeviceMessage 子设备消息
     * @see org.jetlinks.core.message.interceptor.DeviceMessageEncodeInterceptor
     * @see ParallelIntervalHelper
     */
    @Nonnull
    Publisher<? extends EncodedMessage> encode(@Nonnull MessageEncodeContext context);

}
