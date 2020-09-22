package org.jetlinks.core.message.codec;


import javax.annotation.Nonnull;

/**
 * @author zhouhao
 * @see FromDeviceMessageContext
 * @see MessageCodecContext
 * @since 1.0.0
 */
public interface MessageDecodeContext extends MessageCodecContext {

    /**
     * 获取设备上报的原始消息,根据通信协议的不同,消息类型也不同,
     * 在使用时可能需要转换为对应的消息类型
     *
     * @return 原始消息
     * @see EncodedMessage#getPayload()
     * @see MqttMessage
     * @see org.jetlinks.core.message.codec.http.HttpExchangeMessage
     * @see CoapExchangeMessage
     * @since 1.0.0
     */
    @Nonnull
    EncodedMessage getMessage();

}
