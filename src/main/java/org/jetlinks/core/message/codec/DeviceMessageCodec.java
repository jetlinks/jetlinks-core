package org.jetlinks.core.message.codec;

import reactor.core.publisher.Mono;

/**
 * 设备消息转换器,用于对不同协议的消息进行转换
 *
 * @author zhouhao
 * @see org.jetlinks.core.message.interceptor.DeviceMessageCodecInterceptor
 * @since 1.0.0
 */
public interface DeviceMessageCodec extends DeviceMessageEncoder, DeviceMessageDecoder {

    Transport getSupportTransport();

    default Mono<? extends MessageCodecDescription> getDescription() {
        return Mono.empty();
    }
}
