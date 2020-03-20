package org.jetlinks.core.message.codec;

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
     * 编码,将消息进行编码,用于发送到设备端
     *
     * @param context 消息上下文
     * @return 编码结果
     * @see MqttMessage
     * @see org.jetlinks.core.message.Message
     * @see org.jetlinks.core.message.property.ReadPropertyMessage 读取设备属性
     * @see org.jetlinks.core.message.property.WritePropertyMessage 修改设备属性
     * @see org.jetlinks.core.message.function.FunctionInvokeMessage 调用设备功能
     * @see org.jetlinks.core.message.ChildDeviceMessage 子设备消息
     * @see org.jetlinks.core.message.interceptor.DeviceMessageEncodeInterceptor
     */
    @Nonnull
    Publisher<? extends EncodedMessage> encode(@Nonnull MessageEncodeContext context);

}
