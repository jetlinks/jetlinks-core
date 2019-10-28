package org.jetlinks.core.message.codec;

import reactor.core.publisher.Mono;

public interface DeviceMessageEncoder {
    /**
     * 编码,将消息进行编码,用于发送到设备端
     *
     * @param context 消息上下文
     * @return 编码结果
     * @see MqttMessage
     * @see org.jetlinks.core.message.interceptor.DeviceMessageEncodeInterceptor
     */
    Mono<EncodedMessage> encode(MessageEncodeContext context);

}
