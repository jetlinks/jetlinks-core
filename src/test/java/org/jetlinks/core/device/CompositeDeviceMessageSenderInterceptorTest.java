package org.jetlinks.core.device;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.Assert.*;

public class CompositeDeviceMessageSenderInterceptorTest {


    @Test
    public void test() {
        DeviceMessageSenderInterceptor sender = DeviceMessageSenderInterceptor.DO_NOTING;

        WritePropertyMessageReply a = new WritePropertyMessageReply();
        WritePropertyMessageReply b = new WritePropertyMessageReply();
        WritePropertyMessageReply c = new WritePropertyMessageReply();

        sender = sender.andThen(new DeviceMessageSenderInterceptor() {
            @Override
            public <R extends DeviceMessage> Flux<R> afterSent(DeviceOperator device, DeviceMessage message, Flux<R> reply) {
                return Flux.just((R) a);
            }
        });

        sender = sender.andThen(new DeviceMessageSenderInterceptor() {
            @Override
            public <R extends DeviceMessage> Flux<R> afterSent(DeviceOperator device, DeviceMessage message, Flux<R> reply) {
                return Flux.just((R) c);
            }
        });

        sender.afterSent(null, null, Flux.just(b))
                .as(StepVerifier::create)
                .expectNext(c)
                .verifyComplete()
        ;


    }

}