package org.jetlinks.core.defaults;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.ReadPropertyMessageSender;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.utils.IdUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public class DefaultReadPropertyMessageSender implements ReadPropertyMessageSender {

    //设备属性读取消息
    private final ReadPropertyMessage message = new ReadPropertyMessage();

    //设备操作接口,用于发送指令到设备,以及获取配置等相关信
    private final DeviceOperator operator;

    public DefaultReadPropertyMessageSender(DeviceOperator operator) {
        this.operator = operator;
        message.setMessageId(IdUtils.newUUID());
        message.setDeviceId(operator.getDeviceId());
    }

    /**
     * 设置设备读取消息中的属性集合
     *
     * @param property 属性集合
     * @return 读取设备属性消息发送器
     */
    @Override
    public ReadPropertyMessageSender read(Collection<String> property) {
        message.setProperties(new ArrayList<>(property));
        return this;
    }

    @Override
    public ReadPropertyMessageSender custom(Consumer<ReadPropertyMessage> messageConsumer) {
        messageConsumer.accept(message);
        return this;
    }

    /**
     * 添加消息头
     *
     * @param header 消息头标识
     * @param value 消息头存储的值
     * @return 读取设备属性消息发送器
     */
    @Override
    public ReadPropertyMessageSender header(String header, Object value) {
        message.addHeader(header, value);
        return this;
    }

    /**
     * 添加消息ID
     *
     * @param messageId 消息ID
     * @return 读取设备属性消息发送器
     */
    @Override
    public ReadPropertyMessageSender messageId(String messageId) {
        message.setMessageId(messageId);
        return this;
    }

    /**
     * 发送读取设备属性消息
     *
     * @return 读取设备属性消息回复内容
     */
    @Override
    public Flux<ReadPropertyMessageReply> send() {

        return operator
                .messageSender()
                .send(Mono.just(message));
    }
}
