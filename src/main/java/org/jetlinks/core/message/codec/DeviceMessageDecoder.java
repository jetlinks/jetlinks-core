package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.Message;
import reactor.core.publisher.Mono;

public interface DeviceMessageDecoder {
    /**
     * 解码，用于将收到设备上传的消息解码为可读的消息
     *
     * @param context 消息上下文
     * @return 解码结果
     * @see org.jetlinks.core.message.DeviceMessageReply
     * @see org.jetlinks.core.message.property.ReadPropertyMessageReply
     * @see org.jetlinks.core.message.interceptor.DeviceMessageDecodeInterceptor
     */
    Mono<? extends Message> decode(MessageDecodeContext context);
}
