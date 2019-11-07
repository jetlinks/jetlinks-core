package org.jetlinks.core.device;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CompositeDeviceMessageSenderInterceptor implements DeviceMessageSenderInterceptor {
    private List<DeviceMessageSenderInterceptor> interceptors = new CopyOnWriteArrayList<>();

    public void addInterceptor(DeviceMessageSenderInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    @Override
    public Mono<DeviceMessage> preSend(DeviceOperator device, DeviceMessage message) {
        Mono<DeviceMessage> mono = Mono.just(message);

        for (DeviceMessageSenderInterceptor interceptor : interceptors) {
            mono = mono.flatMap(msg -> interceptor.preSend(device, msg));
        }
        return mono;
    }

    @Override
    public <R extends DeviceMessage> Flux<R> afterReply(DeviceOperator device, DeviceMessage message, R reply) {

        Flux<R> flux = Flux.just(reply);

        for (DeviceMessageSenderInterceptor interceptor : interceptors) {
            flux = flux.flatMap(rep -> interceptor.afterReply(device, message, rep));
        }
        return flux;

    }
}
