package org.jetlinks.core.message.codec;

/**
 * 设备消息转换器,用于对不同协议的消息进行转换
 *
 * @author zhouhao
 * @see org.jetlinks.core.message.interceptor.DeviceMessageCodecInterceptor
 * @since 1.0.0
 */
public interface DeviceMessageCodec extends DeviceMessageEncoder, DeviceMessageDecoder {

    Transport getSupportTransport();

}
