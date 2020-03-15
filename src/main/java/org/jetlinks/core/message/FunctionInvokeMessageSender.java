package org.jetlinks.core.message;

import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.function.FunctionParameter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 调用设备功能的消息发送器
 *
 * @author zhouhao
 * @see org.jetlinks.core.device.DeviceMessageSender
 * @see org.jetlinks.core.message.function.FunctionInvokeMessage
 * @see FunctionInvokeMessageReply
 * @since 1.0.0
 */
public interface FunctionInvokeMessageSender {
    FunctionInvokeMessageSender custom(Consumer<FunctionInvokeMessage> messageConsumer);

    FunctionInvokeMessageSender header(String header, Object value);

    FunctionInvokeMessageSender addParameter(FunctionParameter parameter);

    FunctionInvokeMessageSender setParameter(List<FunctionParameter> parameter);

    FunctionInvokeMessageSender messageId(String messageId);

    /**
     * 执行参数校验
     * <pre>
     *     function("door-open")
     *     .validate()
     *     .flatMany(FunctionInvokeMessageSender::send)
     *     .doOnError(IllegalParameterException.class,err->log.error(err.getMessage(),err))
     *     ...
     * </pre>
     *
     * @see Mono#doOnError(Consumer)
     * @see org.jetlinks.core.message.exception.IllegalParameterException
     * @see org.jetlinks.core.message.exception.FunctionUndefinedException
     * @see org.jetlinks.core.message.exception.FunctionIllegalParameterException
     */
    Mono<FunctionInvokeMessageSender> validate();

    /**
     * 发送消息
     *
     * @return 返回结果
     * @see org.jetlinks.core.exception.DeviceOperationException
     * @see org.jetlinks.core.enums.ErrorCode#CLIENT_OFFLINE
     */
    Flux<FunctionInvokeMessageReply> send();

    default Mono<Void> sendAndForget() {
        return header(Headers.sendAndForget, true)
                .async()
                .send()
                .then();
    }

    default FunctionInvokeMessageSender accept(Consumer<FunctionInvokeMessageSender> consumer) {
        consumer.accept(this);
        return this;
    }

    default FunctionInvokeMessageSender addParameter(String name, Object value) {
        return addParameter(new FunctionParameter(name, value));
    }

    default FunctionInvokeMessageSender setParameter(Map<String, Object> parameter) {
        parameter.forEach(this::addParameter);
        return this;
    }

    default FunctionInvokeMessageSender timeout(Duration timeout) {
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
    default FunctionInvokeMessageSender async() {
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
    default FunctionInvokeMessageSender async(Boolean async) {
        return header(Headers.async, async);
    }

    default <T> FunctionInvokeMessageSender header(HeaderKey<T> header, T value) {
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
    default FunctionInvokeMessageSender headers(Map<String, Object> headers) {
        Objects.requireNonNull(headers)
                .forEach(this::header);
        return this;
    }

}
