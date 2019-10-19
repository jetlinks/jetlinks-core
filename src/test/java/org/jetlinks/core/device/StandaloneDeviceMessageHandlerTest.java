package org.jetlinks.core.device;

import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

public class StandaloneDeviceMessageHandlerTest {


    @Test
    public void testSimpleSend() {
        StandaloneDeviceMessageHandler handler = new StandaloneDeviceMessageHandler(id -> DeviceState.online);
        handler.handleMessage("test", msg -> {
            handler.reply(new FunctionInvokeMessageReply().from(msg).success())
                    .subscribe();
        });

        FunctionInvokeMessage message = new FunctionInvokeMessage();
        message.setFunctionId("test");
        message.setMessageId("test");

        Flux<Boolean> successReply = handler.handleReply(message.getMessageId(), Duration.ofSeconds(10))
                .map(DeviceMessageReply::isSuccess);

        handler.send("test", Mono.just(message))
                .as(StepVerifier::create)
                .expectNext(1)
                .verifyComplete();

        successReply.as(StepVerifier::create)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void testNoHandler() {
        StandaloneDeviceMessageHandler handler = new StandaloneDeviceMessageHandler(id -> DeviceState.online);
        FunctionInvokeMessage message = new FunctionInvokeMessage();
        message.setFunctionId("test");
        message.setMessageId("test");

        handler.send("test", Mono.just(message))
                .as(StepVerifier::create)
                .expectNext(0)
                .verifyComplete();


    }

    @Test
    public void testParting() {
        StandaloneDeviceMessageHandler handler = new StandaloneDeviceMessageHandler(id -> DeviceState.online);
        handler.handleMessage("test", msg -> {
            handler.reply(new FunctionInvokeMessageReply()
                    .from(msg)
                    .addHeader(Headers.partMessageId, msg.getMessageId())
                    .addHeader(Headers.shardingPartTotal, 2)
                    .messageId("2")
                    .success())
                    .delayElement(Duration.ofSeconds(1))
                    .flatMap(success ->
                            handler.reply(new FunctionInvokeMessageReply()
                                    .from(msg)
                                    .messageId("1")
                                    .addHeader(Headers.partMessageId, msg.getMessageId())
                                    .addHeader(Headers.shardingPartTotal, 2)
                                    .success()))
                    .subscribe();
        });

        FunctionInvokeMessage message = new FunctionInvokeMessage();
        message.setFunctionId("test");
        message.setMessageId("test");

        Flux<Boolean> successReply = handler
                .handleReply(message.getMessageId(), Duration.ofSeconds(2))
                .map(DeviceMessageReply::isSuccess);

        handler.send("test", Mono.just(message))
                .as(StepVerifier::create)
                .expectNext(1)
                .verifyComplete();

        successReply.
                as(StepVerifier::create)
                .expectNext(true, true)
                .verifyComplete();
    }


}