package org.jetlinks.core.message;

import org.jetlinks.core.message.property.WritePropertyMessageReply;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * 修改设备属性消息发送器
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface WritePropertyMessageSender {

    /**
     * 设置要修改的属性
     * @param property 属性
     * @param value 值
     * @return this
     */
    WritePropertyMessageSender write(String property, Object value);

    /**
     * 将整个map设置为要修改的属性
     * @param properties map属性列表
     * @return this
     */
    default WritePropertyMessageSender write(Map<String, Object> properties) {
        properties.forEach(this::write);
        return this;
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

}
