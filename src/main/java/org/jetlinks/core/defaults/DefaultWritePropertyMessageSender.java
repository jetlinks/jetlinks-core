package org.jetlinks.core.defaults;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.WritePropertyMessageSender;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.utils.IdUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

public class DefaultWritePropertyMessageSender implements WritePropertyMessageSender {

    //修改设备属性消息
    private final WritePropertyMessage message = new WritePropertyMessage();

    //设备操作接口,用于发送指令到设备,以及获取配置等相关信
    private final DeviceOperator operator;

    public DefaultWritePropertyMessageSender(DeviceOperator operator) {
        this.operator = operator;
        message.setMessageId(IdUtils.newUUID());
        message.setDeviceId(operator.getDeviceId());
    }

    @Override
    public WritePropertyMessageSender custom(Consumer<WritePropertyMessage> messageConsumer) {
        messageConsumer.accept(message);
        return this;
    }

    /**
     * 添加消息头
     *
     * @param header 消息头标识
     * @param value 消息头存储的值
     * @return 修改设备属性消息发送器
     */
    @Override
    public WritePropertyMessageSender header(String header, Object value) {
        message.addHeader(header, value);
        return this;
    }

    /**
     * 添加消息ID
     *
     * @param messageId 消息ID
     * @return 修改设备属性消息发送器
     */
    @Override
    public WritePropertyMessageSender messageId(String messageId) {
        message.setMessageId(messageId);
        return this;
    }

    /**
     * 设置设备修改消息中的属性集合
     *
     * @param property 属性集合
     * @return 修改设备属性消息发送器
     */
    @Override
    public WritePropertyMessageSender write(String property, Object value) {
        message.addProperty(property, value);
        return this;
    }

    @Override
    public Mono<WritePropertyMessageSender> validate() {
        Map<String, Object> properties = message.getProperties();

        return operator
                .getMetadata()
                .doOnNext(metadata -> {
                    for (PropertyMetadata meta : metadata.getProperties()) {
                        Object property = properties.get(meta.getId());
                        if (property == null) {
                            continue;
                        }
                        properties.put(meta.getId(), meta.getValueType().validate(property).assertSuccess());
                    }
                }).thenReturn(this);
    }

    /**
     * 发送修改设备属性消息
     *
     * @return 修改设备属性消息回复内容
     */
    @Override
    public Flux<WritePropertyMessageReply> send() {
        return operator.messageSender().send(Mono.just(message));
    }
}
