package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * 消息编码上下文,用于平台向设备发送指令并使用协议包进行编码时,可以从上下文中获取一些参数。
 * 通常此接口可强制转换为{@link ToDeviceMessageContext}。
 *
 * @author zhouhao
 * @see ToDeviceMessageContext
 * @see MessageCodecContext
 * @since 1.0.0
 */
public interface MessageEncodeContext extends MessageCodecContext {

    /**
     * 获取平台下发的给设备的消息指令,根据物模型中定义对应不同的消息类型.
     * 在使用时,需要判断对应的类型进行不同的处理
     *
     * @return 消息
     * @see org.jetlinks.core.message.property.ReadPropertyMessage
     * @see org.jetlinks.core.message.property.WritePropertyMessage
     * @see org.jetlinks.core.message.function.FunctionInvokeMessage
     * @see org.jetlinks.core.message.firmware.UpgradeFirmwareMessage
     * @see org.jetlinks.core.message.firmware.RequestFirmwareMessageReply
     * @since 1.0.0
     */
    @Nonnull
    Message getMessage();

    /**
     * 直接回复消息给平台.在类似通过http接入时,下发指令可能是一个同步操作,则可以通过此方法直接回复平台.
     *
     * @param replyMessage 消息流
     * @return void
     * @since 1.0.2
     */
    @Nonnull
    default Mono<Void> reply(@Nonnull Publisher<? extends DeviceMessage> replyMessage) {
        return Mono.empty();
    }

    /**
     * {@link this#reply(Publisher)}
     *
     * @param messages 消息
     * @return void
     * @since 1.1.1
     */
    @Nonnull
    default Mono<Void> reply(@Nonnull Collection<? extends DeviceMessage> messages) {
        return reply(Flux.fromIterable(messages));
    }

    /**
     * {@link this#reply(Publisher)}
     *
     * @param messages 消息
     * @return void
     * @since 1.1.1
     */
    @Nonnull
    default Mono<Void> reply(@Nonnull DeviceMessage... messages) {
        return reply(Flux.fromArray(messages));
    }
}
