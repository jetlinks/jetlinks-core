package org.jetlinks.core.defaults;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.DeviceMessageSender;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceOperationBroker;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

@Slf4j
public class DefaultDeviceMessageSender implements DeviceMessageSender {

    private DeviceOperationBroker handler;

    private DeviceOperator operator;

    @Setter
    @Getter
    private long defaultTimeout = TimeUnit.SECONDS.toMillis(10);

    public DefaultDeviceMessageSender(DeviceOperationBroker handler,
                                      DeviceOperator operator) {
        this.handler = handler;
        this.operator = operator;
    }

    @Override
    public <R extends DeviceMessageReply> Flux<R> send(Publisher<RepayableDeviceMessage<R>> message) {
        return send(message, this::convertReply);
    }

    protected <T extends DeviceMessageReply> T convertReply(Object obj) {
        // TODO: 2019-10-18 更好的转化实现
        return ((T) obj);
    }

    private <R extends DeviceMessage> Flux<R> logReply(DeviceMessage msg, Flux<R> flux) {
        if (log.isDebugEnabled()) {
            return flux.doOnNext(r -> {
                if (log.isDebugEnabled()) {
                    log.debug("receive device[{}] message[{}]: {}", operator.getDeviceId(), r.getMessageId(), r);
                }
            }).doOnComplete(() -> {
                if (log.isDebugEnabled()) {
                    log.debug("complete receive device[{}] message[{}]", operator.getDeviceId(), msg.getMessageId());
                }
            }).doOnCancel(() -> {
                if (log.isDebugEnabled()) {
                    log.debug("cancel receive device[{}] message[{}]", operator.getDeviceId(), msg.getMessageId());
                }
            });
        }
        return flux;
    }

    public <R extends DeviceMessage> Flux<R> send(Publisher<? extends DeviceMessage> message, Function<Object, R> replyMapping) {
        return Mono.zip(
                operator.getConnectionServerId().defaultIfEmpty("_"), //当前设备连接的服务器ID
                operator.getProtocol()
                        .flatMap(ProtocolSupport::getSenderInterceptor)     //拦截器
                        .defaultIfEmpty(DeviceMessageSenderInterceptor.DO_NOTING))
                .flatMapMany(serverAndInterceptor -> {
                    String server = serverAndInterceptor.getT1();
                    DeviceMessageSenderInterceptor interceptor = serverAndInterceptor.getT2();
                    return Flux.from(message)
                            .flatMap(msg -> interceptor.preSend(operator, msg))
                            .concatMap(msg -> {
                                if ("-".equals(server)) {
                                    return interceptor.afterSent(operator, msg, Flux.error(new DeviceOperationException(ErrorCode.CLIENT_OFFLINE)));
                                }
                                //处理来自设备的回复.
                                Flux<R> replyStream = handler
                                        .handleReply(msg.getMessageId(), Duration.ofMillis(msg.getHeader(Headers.timeout).orElse(defaultTimeout)))
                                        .map(replyMapping)
                                        .onErrorMap(TimeoutException.class, timeout -> new DeviceOperationException(ErrorCode.TIME_OUT, timeout))
                                        .as(flux -> this.logReply(msg, flux));
                                //发送消息到设备连接的服务器
                                return handler
                                        .send(server, Mono.just(msg))
                                        .defaultIfEmpty(-1)
                                        .flatMap(len -> {
                                            //设备未连接到服务器
                                            if (len == 0) {
                                                //尝试发起状态检查,已同步设备的真实状态
                                                return operator
                                                        .checkState()
                                                        .then(Mono.error(new DeviceOperationException(ErrorCode.CLIENT_OFFLINE)));
                                            } else if (len == -1) {
                                                return Mono.error(new DeviceOperationException(ErrorCode.CLIENT_OFFLINE));
                                            }
                                            log.debug("send device[{}] message complete", operator.getDeviceId());
                                            return Mono.just(true);
                                        })
                                        .thenMany(replyStream)
                                        .as(result -> interceptor.afterSent(operator, msg, result))
                                        ;
                            });
                });

    }

    @Override
    public FunctionInvokeMessageSender invokeFunction(String function) {
        return new DefaultFunctionInvokeMessageSender(operator, function);
    }

    @Override
    public ReadPropertyMessageSender readProperty(String... property) {
        return new DefaultReadPropertyMessageSender(operator)
                .read(property);
    }

    @Override
    public WritePropertyMessageSender writeProperty() {
        return new DefaultWritePropertyMessageSender(operator);
    }
}
