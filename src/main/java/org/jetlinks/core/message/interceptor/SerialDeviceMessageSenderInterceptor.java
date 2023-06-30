package org.jetlinks.core.message.interceptor;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.utils.SerialFlux;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.Map;

public class SerialDeviceMessageSenderInterceptor implements DeviceMessageSenderInterceptor {

    public static final SerialDeviceMessageSenderInterceptor GLOBAL = new SerialDeviceMessageSenderInterceptor();


    final Map<Object, SerialFlux<DeviceMessage>> pending = Caffeine
            .newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .<Object, SerialFlux<DeviceMessage>>build()
            .asMap();

    protected boolean needSerial(DeviceMessage message) {
        return true;
    }

    protected Object getSerialKey(DeviceMessage message) {
        return Tuples.of(message.getDeviceId(), message.getMessageType());
    }

    @Override
    public Flux<DeviceMessage> doSend(DeviceOperator device, DeviceMessage source, Flux<DeviceMessage> sender) {
        if (!needSerial(source)) {
            return sender;
        }

        Object key = getSerialKey(source);

        return pending
                .computeIfAbsent(key, ignore -> new SerialFlux<>())
                .join(sender);
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
