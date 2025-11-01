package org.jetlinks.core.defaults;

import lombok.AllArgsConstructor;
import org.jetlinks.core.device.DeviceMessageSender;
import org.jetlinks.core.message.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@AllArgsConstructor
class RpcDeviceMessageSender implements DeviceMessageSender {

    private final DefaultDeviceOperator device;

    @Override
    public <R extends DeviceMessageReply> Flux<R> send(Publisher<RepayableDeviceMessage<R>> message) {
        return Flux
            .from(message)
            .flatMap(msg -> device.rpc().call(msg));
    }

    @Override
    public <R extends DeviceMessage> Flux<R> send(Publisher<? extends DeviceMessage> message, Function<Object, R> replyMapping) {
        return Flux
            .from(message)
            .flatMap(msg -> device.rpc().call(msg))
            .map(replyMapping);
    }

    @Override
    public <R extends DeviceMessage> Flux<R> send(DeviceMessage message) {
        return device
            .rpc()
            .call(message)
            .map(msg -> (R) msg);
    }

    @Override
    public FunctionInvokeMessageSender invokeFunction(String function) {
        return new DefaultFunctionInvokeMessageSender(device, function);
    }


    @Override
    public ReadPropertyMessageSender readProperty(String... property) {
        return new DefaultReadPropertyMessageSender(device)
            .read(property);
    }


    @Override
    public WritePropertyMessageSender writeProperty() {
        return new DefaultWritePropertyMessageSender(device);
    }
}
