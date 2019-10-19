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

    private ReadPropertyMessage message = new ReadPropertyMessage();

    private DeviceOperator operator;

    public DefaultReadPropertyMessageSender(DeviceOperator operator) {
        this.operator = operator;
        message.setMessageId(IdUtils.newUUID());
        message.setDeviceId(operator.getDeviceId());
    }

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

    @Override
    public ReadPropertyMessageSender header(String header, Object value) {
        message.addHeader(header, value);
        return this;
    }

    @Override
    public ReadPropertyMessageSender messageId(String messageId) {
        message.setMessageId(messageId);
        return this;
    }

    @Override
    public Flux<ReadPropertyMessageReply> send() {

        return operator
                .messageSender()
                .send(Mono.just(message));
    }
}
