package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.Message;
import reactor.core.publisher.Mono;

/**
 * 设备消息转换器,用于对不同协议的消息进行转换
 *
 * @author zhouhao
 * @see org.jetlinks.core.message.interceptor.DeviceMessageCodecInterceptor
 * @since 1.0.0
 */
public interface DeviceMessageCodec {

    Transport getSupportTransport();

    /**
     * 编码,将消息进行编码,用于发送到设备端
     *
     * @param context 消息上下文
     * @return 编码结果
     * @see MqttMessage
     * @see org.jetlinks.core.message.interceptor.DeviceMessageEncodeInterceptor
     */
    Mono<EncodedMessage> encode(MessageEncodeContext context);

    /**
     * 解码，用于将收到设备上传的消息解码为可读的消息
     *
     * @param context 消息上下文
     * @return 解码结果
     * @see org.jetlinks.core.message.DeviceMessageReply
     * @see org.jetlinks.core.message.property.ReadPropertyMessageReply
     * @see org.jetlinks.core.message.interceptor.DeviceMessageDecodeInterceptor
     */
    <T extends Message> Mono<T> decode(MessageDecodeContext context);
}
