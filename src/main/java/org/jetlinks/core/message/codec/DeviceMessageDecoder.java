package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.Message;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;

public interface DeviceMessageDecoder {
    /**
     * 解码，用于将收到设备上传的消息解码为可读的消息
     *
     * @param context 消息上下文
     * @return 解码结果
     * @see MqttMessage
     * @see org.jetlinks.core.message.DeviceMessageReply
     * @see org.jetlinks.core.message.property.ReadPropertyMessageReply
     * @see org.jetlinks.core.message.function.FunctionInvokeMessageReply
     * @see org.jetlinks.core.message.ChildDeviceMessageReply
     * @see org.jetlinks.core.message.DeviceOnlineMessage
     * @see org.jetlinks.core.message.DeviceOfflineMessage
     * @see org.jetlinks.core.message.interceptor.DeviceMessageDecodeInterceptor
     */
    @Nonnull
    Publisher<? extends Message> decode(@Nonnull MessageDecodeContext context);
}
