package org.jetlinks.core.message;

import org.jetlinks.core.message.property.ReadPropertyMessageReply;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;

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
     * 执行发送
     *
     * @return 异步完成阶段
     * @see org.jetlinks.core.device.DeviceMessageSender#send(RepayableDeviceMessage)
     * @see CompletionStage
     * @see CompletionStage#toCompletableFuture()
     */
    CompletionStage<ReadPropertyMessageReply> send();

}
