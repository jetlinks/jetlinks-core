package org.jetlinks.core.message;

import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
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
    Mono<WritePropertyMessageReply> retrieveReply();

    /**
     * 执行发送
     *
     * @return 异步完成阶段
     * @see org.jetlinks.core.device.DeviceMessageSender#send(RepayableDeviceMessage)
     * @see CompletionStage
     * @see CompletionStage#toCompletableFuture()
     */
    Mono<WritePropertyMessageReply> send();


}
