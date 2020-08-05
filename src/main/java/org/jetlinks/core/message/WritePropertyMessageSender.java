package org.jetlinks.core.message;

import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 修改设备属性消息发送器
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface WritePropertyMessageSender {

    WritePropertyMessageSender custom(Consumer<WritePropertyMessage> messageConsumer);

    WritePropertyMessageSender header(String header, Object value);

    WritePropertyMessageSender messageId(String messageId);

    WritePropertyMessageSender write(String property, Object value);

    Mono<WritePropertyMessageSender> validate();

    /**
     * 发送消息
     *
     * @return 返回结果
     * @see org.jetlinks.core.exception.DeviceOperationException
     * @see org.jetlinks.core.enums.ErrorCode#CLIENT_OFFLINE
     */
    Flux<WritePropertyMessageReply> send();

    default Mono<Void> sendAndForget() {
        return header(Headers.sendAndForget, true)
                .async()
                .send()
                .then();
    }

    default WritePropertyMessageSender write(Map<String, Object> properties) {

        properties.forEach(this::write);

        return this;
    }


    default WritePropertyMessageSender accept(Consumer<WritePropertyMessageSender> consumer) {
        consumer.accept(this);
        return this;
    }

    default WritePropertyMessageSender timeout(Duration timeout) {
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
    default WritePropertyMessageSender async() {
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
    default WritePropertyMessageSender async(Boolean async) {
        return header(Headers.async, async);
    }

    default <T> WritePropertyMessageSender header(HeaderKey<T> header, T value) {
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
    default WritePropertyMessageSender headers(Map<String, Object> headers) {
        Objects.requireNonNull(headers)
                .forEach(this::header);
        return this;
    }


}
