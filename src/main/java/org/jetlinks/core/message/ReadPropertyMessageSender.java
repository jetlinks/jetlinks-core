package org.jetlinks.core.message;

import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 读取设备属性消息发送器
 *
 * @author zhouhao
 * @see org.jetlinks.core.message.property.ReadPropertyMessage
 * @see ReadPropertyMessageReply
 * @since 1.0.0
 */
public interface ReadPropertyMessageSender {

    ReadPropertyMessageSender custom(Consumer<ReadPropertyMessage> messageConsumer);

    ReadPropertyMessageSender header(String header, Object value);

    ReadPropertyMessageSender messageId(String messageId);

    /**
     * 发送消息
     *
     * @return 返回结果
     * @see org.jetlinks.core.exception.DeviceOperationException
     * @see org.jetlinks.core.enums.ErrorCode#CLIENT_OFFLINE
     */
    Flux<ReadPropertyMessageReply> send();

    default Mono<Void> sendAndForget() {
        return header(Headers.sendAndForget, true).send().then();
    }

    ReadPropertyMessageSender read(Collection<String> property);

    default ReadPropertyMessageSender read(String... property) {
        return read(Arrays.asList(property));
    }

    default ReadPropertyMessageSender accept(Consumer<ReadPropertyMessageSender> consumer) {
        consumer.accept(this);
        return this;
    }

    default ReadPropertyMessageSender timeout(Duration timeout) {
        return header(Headers.timeout, timeout.toMillis());
    }

    /**
     * 设置调用此功能为异步执行, 当消息发送到设备后,立即返回{@link org.jetlinks.core.enums.ErrorCode#REQUEST_HANDLING},而不等待设备返回结果.
     *
     * <code>{"success":true,"code":"REQUEST_HANDLING"}</code>
     *
     * @return this
     * @see Headers#async
     */
    default ReadPropertyMessageSender async() {
        return this.async(true);
    }

    /**
     * 设置是否异步
     *
     * @param async 是否异步
     * @return this
     * @see this#async(Boolean)
     * @see Headers#async
     */
    default ReadPropertyMessageSender async(Boolean async) {
        return header(Headers.async, async);
    }

    default <T> ReadPropertyMessageSender header(HeaderKey<T> header, T value) {
        return header(header.getKey(), value);
    }

    /**
     * 添加多个header到message中
     *
     * @param headers 多个headers
     * @return this
     * @see this#header(String, Object)
     * @see DeviceMessage#addHeader(String, Object)
     * @see Headers
     */
    default ReadPropertyMessageSender headers(Map<String, Object> headers) {
        Objects.requireNonNull(headers)
                .forEach(this::header);
        return this;
    }

}
