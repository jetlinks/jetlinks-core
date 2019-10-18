package org.jetlinks.core.message;

import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 读取设备属性消息发送器
 *
 * @author zhouhao
 * @see org.jetlinks.core.message.property.ReadPropertyMessage
 * @see ReadPropertyMessageReply
 * @since 1.0.0
 */
public interface ReadPropertyMessageSender {

    /**
     * 自定义消息
     *
     * @param messageConsumer consumer
     * @return this
     */
    ReadPropertyMessageSender custom(Consumer<ReadPropertyMessage> messageConsumer);

    /**
     * 设置要读取的属性列表
     *
     * @param property 属性列表
     * @return this
     * @see this#read(List)
     */
    default ReadPropertyMessageSender read(String... property) {
        return read(Arrays.asList(property));
    }

    /**
     * 指定messageId,如果不指定,将使用uuid生成一个.
     * <p>
     * ⚠️ messageId 应该全局唯一,且不能消息16位
     *
     * @param messageId messageId
     * @return this
     */
    ReadPropertyMessageSender messageId(String messageId);

    /**
     * 设置要读取的属性列表
     *
     * @param properties 属性列表
     * @return this
     * @see this#read(List)
     */
    ReadPropertyMessageSender read(List<String> properties);

    /**
     * 执行发送,如果获取结果超时,只有手动调用{@link CompletableFuture#cancel(boolean)}取消获取,
     * 之后才能通过{@link this#retrieveReply()}重新获取结果.
     *
     * @return 异步完成阶段
     * @see org.jetlinks.core.device.DeviceMessageSender#send(RepayableDeviceMessage)
     * @see CompletionStage
     * @see CompletionStage#toCompletableFuture()
     */
    Mono<ReadPropertyMessageReply> send();

    /**
     * 尝试重新获取返回值
     *
     * @return 获取结果
     * @see org.jetlinks.core.device.DeviceMessageSender#retrieveReply(String, Supplier)
     * @see org.jetlinks.core.enums.ErrorCode#NO_REPLY
     */
    Mono<ReadPropertyMessageReply> retrieveReply();

    /**
     * 添加header到message中
     *
     * @param header header
     * @param value  值
     * @return this
     * @see DeviceMessage#addHeader(String, Object)
     */
    ReadPropertyMessageSender header(String header, Object value);

    /**
     * 添加多个header到message中
     *
     * @param headers 多个headers
     * @return this
     * @see this#header(String, Object)
     * @see DeviceMessage#addHeader(String, Object)
     */
    default ReadPropertyMessageSender headers(Map<String, Object> headers) {
        Objects.requireNonNull(headers)
                .forEach(this::header);
        return this;
    }

    default ReadPropertyMessageSender timeout(int timeoutSeconds) {
        return custom(message -> message.addHeader("timeout", timeoutSeconds));
    }

}
