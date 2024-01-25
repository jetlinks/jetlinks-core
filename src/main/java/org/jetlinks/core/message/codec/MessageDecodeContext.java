package org.jetlinks.core.message.codec;


import org.jetlinks.core.message.DeviceMessage;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Function;

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

    /**
     * 手动调用处理设备消息,此操作可以感知到设备消息的处理结果. 可通过{@link Mono#onErrorResume(Function)}来处理错误.
     * <pre>{@code
     *
     *   DeviceMessage msg = doDecode(context);
     *
     *   return context
     *          .handleMessage(msg)
     *          //应答错误
     *          .onErrorResume(err-> return ackError(context).then(Mono.error(err)))
     *          //应答成功
     *          .then(ackSuccess(context))
     *          //返回空,因为handleMessage已经手动处理过了
     *          .then(Mono.empty())
     *
     * }
     * @param message 设备消息
     * @see Mono#onErrorResume(Function)
     * @see Mono#doOnError(Consumer)
     * @return void
     * @since 1.2.2
     */
    default Mono<Void> handleMessage(DeviceMessage message) {
        return Mono.error(new UnsupportedOperationException("handleMessage not supported"));
    }

}
