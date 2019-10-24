package org.jetlinks.core.defaults;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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

    private DeviceMessageSenderInterceptor interceptor;

    @Setter
    @Getter
    private long defaultTimeout = TimeUnit.SECONDS.toMillis(10);

    public DefaultDeviceMessageSender(DeviceOperationBroker handler, DeviceOperator operator, DeviceMessageSenderInterceptor interceptor) {
        this.handler = handler;
        this.operator = operator;
        this.interceptor = interceptor;
    }

    @Override
    public <R extends DeviceMessageReply> Flux<R> send(Publisher<RepayableDeviceMessage<R>> message) {
        return send(message, this::convertReply);
    }

    protected <T extends DeviceMessageReply> T convertReply(Object obj) {
        // TODO: 2019-10-18
        return ((T) obj);
    }

    public <R extends DeviceMessageReply> Flux<R> send(Publisher<? extends DeviceMessage> message, Function<Object, R> replyMapping) {
        return operator
                .getConnectionServerId()
                .switchIfEmpty(Mono.error(() -> new DeviceOperationException(ErrorCode.CLIENT_OFFLINE)))
                .flatMapMany(server -> Flux.from(message)
                        .flatMap(msg -> interceptor.preSend(operator, msg))
                        .concatMap(msg -> {
                            Flux<R> replyStream = handler
                                    .handleReply(msg.getMessageId(), Duration.ofMillis(msg.getHeader(Headers.timeout).orElse(defaultTimeout)))
                                    .map(replyMapping)
                                    .flatMap(reply -> interceptor.afterReply(operator, msg, reply))
                                    .onErrorMap(TimeoutException.class, timeout -> new DeviceOperationException(ErrorCode.TIME_OUT, timeout))
                                    .as(flux -> {
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
                                    });
                            return handler
                                    .send(server, Mono.just(msg))
                                    .switchIfEmpty(Mono.just(-1))
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
                                    .thenMany(replyStream);
                        }));

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
