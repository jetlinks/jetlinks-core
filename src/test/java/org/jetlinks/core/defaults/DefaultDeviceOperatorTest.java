package org.jetlinks.core.defaults;

import org.jetlinks.core.Value;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.device.*;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.utils.IdUtils;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;

public class DefaultDeviceOperatorTest {

    private TestDeviceRegistry registry;

    private StandaloneDeviceMessageBroker deviceMessageBroker;

    @Before
    public void init() {
        registry = new TestDeviceRegistry(new TestProtocolSupport(),
                deviceMessageBroker = new StandaloneDeviceMessageBroker());

        registry.addInterceptor(
                new DeviceMessageSenderInterceptor() {
                    @Override
                    public <R extends DeviceMessage> Flux<R> afterSent(DeviceOperator device, DeviceMessage message, Flux<R> reply) {
                        return reply
                                .doOnNext(msg->{
                                    msg.addHeader("1",2);
                                });
                    }
                }
        );
    }

    @Test
    public void testParent(){

        deviceMessageBroker
                .handleGetDeviceState("test2", idStream -> Flux.from(idStream)
                        .map(s -> new DeviceStateInfo(s, DeviceState.online)));

        deviceMessageBroker.handleSendToDeviceMessage("test2")
                .cast(ChildDeviceMessage.class)
                .subscribe(msg->{
                    ChildDeviceMessageReply reply=msg.newReply();
                    reply.setChildDeviceId(msg.getChildDeviceId());
                    reply.setChildDeviceMessage(new ReadPropertyMessageReply()
                            .messageId(msg.getMessageId())
                            .success(Collections.singletonMap("name","test")));

                    deviceMessageBroker.reply(reply)
                    .subscribe();
                });
        registry.register(DeviceInfo.builder()
                .id("test-gateway")
                .build())
                .flatMap(operator -> operator.online("test2","test"))
                .then()
                .as(StepVerifier::create)
                .expectComplete()
                .verify();

        registry.register(DeviceInfo.builder()
                .id("test-children")
                .build())
                .flatMap(operator -> operator.setConfig(DeviceConfigKey.parentGatewayId,"test-gateway"))
                .then()
                .as(StepVerifier::create)
                .expectComplete()
                .verify();

        registry.getDevice("test-children")
                .flatMap(DeviceOperator::checkState)
                .as(StepVerifier::create)
                .expectNext(DeviceState.online)
                .verifyComplete();

        registry.getDevice("test-children")
                .map(DeviceOperator::messageSender)
                .flatMapMany(sender-> sender.readProperty("name")
                         .send())
                .take(1)
                .map(ReadPropertyMessageReply::getProperties)
                .map(prop->prop.get("name"))
                .as(StepVerifier::create)
                .expectNext("test")
                .verifyComplete();


    }

    @Test
    public void testMessageSend() {

        deviceMessageBroker.handleSendToDeviceMessage("test")
                .cast(RepayableDeviceMessage.class)
                .subscribe(msg -> deviceMessageBroker
                        .reply(msg.newReply().success())
                        .subscribe());

        registry.register(DeviceInfo.builder()
                .id("test")
                .build())
                .zipWhen(operator -> operator.online("test", "test"), (o, r) -> o)
                .log()
                .map(DeviceOperator::messageSender)
                .map(sender -> sender.invokeFunction("test").addParameter("arg0", 1))
                .flatMapMany(FunctionInvokeMessageSender::send)
                .map(FunctionInvokeMessageReply::isSuccess)
                .as(StepVerifier::create)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void testCheckHandledState() {

        deviceMessageBroker
                .handleGetDeviceState("test", idStream -> Flux.from(idStream)
                        .map(s -> new DeviceStateInfo(s, DeviceState.unknown)));

        registry.register(DeviceInfo.builder()
                .id("test")
                .build())
                .doOnNext(operator -> operator.online("test", "test").subscribe())
                .flatMap(DeviceOperator::checkState)
                .log()
                .as(StepVerifier::create)
                .expectNext(DeviceState.unknown)
                .verifyComplete();
    }

    @Test
    public void testCheckStateEmpty() {
        deviceMessageBroker
                .handleGetDeviceState("test", idStream -> Flux.empty());

        registry.register(DeviceInfo.builder()
                .id("test")
                .build())
                .doOnNext(operator -> operator.online("test", "test").subscribe())
                .flatMap(DeviceOperator::checkState)
                .log()
                .as(StepVerifier::create)
                .expectNext(DeviceState.online)
                .verifyComplete();
    }
    @Test
    public void testCheckState() {
        deviceMessageBroker
                .handleGetDeviceState("test", idStream -> Flux.from(idStream)
                        .map(s -> new DeviceStateInfo(s, DeviceState.offline)));

        registry.register(DeviceInfo.builder()
                .id("test")
                .build())
                .doOnNext(operator -> operator.online("test", "test").subscribe())
                .flatMap(DeviceOperator::checkState)
                .log()
                .as(StepVerifier::create)
                .expectNext(DeviceState.offline)
                .verifyComplete();
    }

    @Test
    public void testMessageSendPartingReply() {

        deviceMessageBroker.handleSendToDeviceMessage("test")
                .cast(RepayableDeviceMessage.class)
                .subscribe(deviceMessage -> Flux.range(0, 5)
                        .map(i -> (deviceMessage).newReply()
                                .messageId(IdUtils.newUUID())
                                .addHeader(Headers.fragmentBodyMessageId, deviceMessage.getMessageId())
                                .addHeader(Headers.fragmentNumber, 5)
                                .addHeader(Headers.fragmentPart, i)
                                .success())
                        .delayElements(Duration.ofMillis(500))
                        .flatMap(deviceMessageBroker::reply)
                        .subscribe());

        registry.register(DeviceInfo.builder()
                .id("test")
                .build())
                .zipWhen(operator -> operator.online("test", "test"), (o, r) -> o)
                .map(DeviceOperator::messageSender)
                .map(sender -> sender.invokeFunction("test")
                        .addParameter("arg0", 1))
                .flatMapMany(FunctionInvokeMessageSender::send)
                .map(FunctionInvokeMessageReply::isSuccess)
                .as(StepVerifier::create)
                .expectNext(true, true, true, true, true)
                .verifyComplete();
    }

    @Test
    public void testConfig() {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setId("test");
        registry.register(deviceInfo)
                .then(registry.getDevice("test"))
                .flatMap(operator -> operator.setConfig(ConfigKey.of("clientId").value("test")))
                .then(registry.getDevice("test"))
                .flatMap(operator -> operator.getConfig("clientId"))
                .map(Value::asString)
                .as(StepVerifier::create)
                .expectNext("test")
                .verifyComplete();
    }

    @Test
    public void testConfigMulti() {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setId("test");
        registry.register(deviceInfo)
                .then(registry.getDevice("test"))
                .flatMap(operator -> operator.setConfigs(ConfigKey.of("clientId").value("test"), ConfigKey.of("password").value("test")))
                .then(registry.getDevice("test"))
                .flatMap(operator -> operator.getConfigs("clientId", "password"))
                .map(values -> values.getValue("clientId").isPresent() && values.getValue("password").isPresent())
                .as(StepVerifier::create)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void testConfigParentPart() {
        registry.register(ProductInfo.builder()
                .id("test-prod")
                .build())
                .flatMap(po -> po.setConfig("password", "test"))
                .then(
                        registry.register(DeviceInfo.builder()
                                .id("test")
                                .productId("test-prod")
                                .build())
                )
                .then(registry.getDevice("test"))
                .flatMap(operator -> operator.setConfigs(ConfigKey.of("clientId").value("test")))
                .then(registry.getDevice("test"))
                .flatMap(operator -> operator.getConfigs("clientId", "password"))
                .map(values -> values.getValue("clientId").isPresent() && values.getValue("password").isPresent())
                .as(StepVerifier::create)
                .expectNext(true)
                .verifyComplete();

    }
}