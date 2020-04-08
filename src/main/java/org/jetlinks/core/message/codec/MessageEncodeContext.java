package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * @author zhouhao
 * @see ToDeviceMessageContext
 * @since 1.0.0
 */
public interface MessageEncodeContext extends MessageCodecContext {

    @Nonnull
    Message getMessage();

    /**
     * 直接回复消息
     *
     * @param replyMessage 消息流
     * @return 回复结果
     * @since 1.0.2
     */
    @Nonnull
    default Mono<Void> reply(@Nonnull Publisher<? extends DeviceMessage> replyMessage) {
        return Mono.empty();
    }
}
