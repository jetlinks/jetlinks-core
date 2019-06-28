package org.jetlinks.core.message;

import io.vavr.control.Try;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 修改设备属性消息发送器
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface WritePropertyMessageSender {

    /**
     * 设置要修改的属性
     *
     * @param property 属性
     * @param value    值
     * @return this
     */
    WritePropertyMessageSender write(String property, Object value);

    /**
     * 自定义消息
     *
     * @param messageConsumer consumer
     * @return this
     */
    WritePropertyMessageSender custom(Consumer<WritePropertyMessage> messageConsumer);

    default WritePropertyMessageSender timeout(int timeoutSeconds) {
        return custom(message -> message.addHeader("timeout", timeoutSeconds));
    }

    /**
     * 添加header到message中
     *
     * @param header header
     * @param value  值
     * @return this
     * @see DeviceMessage#addHeader(String, Object)
     */
    WritePropertyMessageSender header(String header, Object value);

    /**
     * 添加多个header到message中
     *
     * @param headers 多个headers
     * @return this
     * @see this#header(String, Object)
     * @see DeviceMessage#addHeader(String, Object)
     */
    default WritePropertyMessageSender headers(Map<String, Object> headers) {
        Objects.requireNonNull(headers)
                .forEach(this::header);
        return this;
    }

    /**
     * 将整个map设置为要修改的属性
     *
     * @param properties map属性列表
     * @return this
     */
    default WritePropertyMessageSender write(Map<String, Object> properties) {
        properties.forEach(this::write);
        return this;
    }

    /**
     * 尝试重新获取返回值
     *
     * @return 获取结果
     * @see org.jetlinks.core.device.DeviceMessageSender#retrieveReply(String, Supplier)
     * @see org.jetlinks.core.enums.ErrorCode#NO_REPLY
     */
    CompletionStage<WritePropertyMessageReply> retrieveReply();

    /**
     * 请看{@link this#retrieveReply()} 和 {@link Try}
     *
     * @param timeout  超时时间
     * @param timeUnit 超时时间单位
     * @return Try
     * @see this#retrieveReply()
     */
    default Try<WritePropertyMessageReply> tryRetrieveReply(long timeout, TimeUnit timeUnit) {
        return Try.of(() -> this.retrieveReply().toCompletableFuture().get(timeout, timeUnit));
    }

    /**
     * 执行发送
     *
     * @return 异步完成阶段
     * @see org.jetlinks.core.device.DeviceMessageSender#send(RepayableDeviceMessage)
     * @see CompletionStage
     * @see CompletionStage#toCompletableFuture()
     */
    CompletionStage<WritePropertyMessageReply> send();

    /**
     * 发送消息并返回{@link Try},可进行函数式操作.
     *
     * <pre>
     *     sender.writeProperty()
     *            .write("ver","1.0")
     *            .trySend(10,TimeUnit.SECONDS)
     *            .recoverWith(TimeoutException.class,r->failureTry(ErrorCode.TIME_OUT))
     *            .get();
     * </pre>
     *
     * @param timeout  超时时间
     * @param timeUnit 超时时间单位
     * @return Try
     */
    default Try<WritePropertyMessageReply> trySend(long timeout, TimeUnit timeUnit) {
        return Try.of(() -> {
            CompletableFuture<WritePropertyMessageReply> stage = send().toCompletableFuture();
            try {
                return stage.get(timeout, timeUnit);
            } catch (TimeoutException e) {
                //超时后取消执行任务
                stage.cancel(true);
                throw e;
            }
        });
    }

}
