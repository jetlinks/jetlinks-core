package org.jetlinks.core.device;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CompositeDeviceMessageSenderInterceptor implements DeviceMessageSenderInterceptor {
    private final List<DeviceMessageSenderInterceptor> interceptors = new CopyOnWriteArrayList<>();

    public void addInterceptor(DeviceMessageSenderInterceptor interceptor) {
        interceptors.add(interceptor);
        //重新排序
        interceptors.sort(Comparator.comparingInt(DeviceMessageSenderInterceptor::getOrder));
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
    public <R extends DeviceMessage> Flux<R> afterSent(DeviceOperator device, DeviceMessage message, Flux<R> reply) {

        Flux<R> flux = reply;

        for (DeviceMessageSenderInterceptor interceptor : interceptors) {
            flux = interceptor.afterSent(device, message, flux);
        }
        return flux;

    }
}
