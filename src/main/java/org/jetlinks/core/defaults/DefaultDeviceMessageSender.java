package org.jetlinks.core.defaults;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.device.DeviceMessageHandler;
import org.jetlinks.core.device.DeviceMessageSender;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Slf4j
public class DefaultDeviceMessageSender implements DeviceMessageSender {

    private DeviceMessageHandler handler;

    private DeviceOperator operator;

    private DeviceMessageSenderInterceptor interceptor;

    @Setter
    @Getter
    private long defaultTimeout = TimeUnit.SECONDS.toMillis(10);

    public DefaultDeviceMessageSender(DeviceMessageHandler handler, DeviceOperator operator, DeviceMessageSenderInterceptor interceptor) {
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

    @Override
    public <R extends DeviceMessageReply> Flux<R> send(Publisher<? extends DeviceMessage> message, Function<Object, R> replyMapping) {
        return Flux.create(replySink -> {
            AtomicInteger awaitCounter = new AtomicInteger();
            replySink.onDispose(
                    operator.getConnectionServerId()
                            .flatMap(serverId ->
                                    Flux.from(message)
                                            .flatMap(msg -> interceptor.preSend(operator, msg))
                                            .<DeviceMessage>handle((msg, sink) -> {
                                                awaitCounter.incrementAndGet();
                                                //等待回复,无论同步异步消息,正常情况下设备网关服务都会返回消息
                                                Disposable disposable = handler
                                                        .handleReply(msg.getMessageId(), Duration.ofMillis(msg.getHeader(Headers.timeout).orElse(defaultTimeout)))
                                                        .map(replyMapping)
                                                        .onErrorMap(TimeoutException.class, timeout -> new DeviceOperationException(ErrorCode.TIME_OUT, timeout))
                                                        .doOnNext(r -> {
                                                            if (log.isInfoEnabled()) {
                                                                log.info("receive device[{}] message[{}]: {}", operator.getDeviceId(), r.getMessageId(), r);
                                                            }
                                                        })
                                                        .doOnComplete(() -> {
                                                            //一个消息请求,多条消息回复的场景,等所有消息都完成才完成整个流
                                                            if (awaitCounter.decrementAndGet() <= 0) {
                                                                replySink.complete();
                                                                if (log.isInfoEnabled()) {
                                                                    log.info("complete receive device[{}] message[{}]", operator.getDeviceId(), msg.getMessageId());
                                                                }
                                                            }
                                                        })
                                                        .subscriberContext(replySink.currentContext())
                                                        .doOnError(replySink::error)
                                                        .flatMap(reply -> interceptor.afterReply(operator, msg, reply))
                                                        .doFinally((s) -> {
                                                            if (log.isInfoEnabled()) {
                                                                if (replySink.isCancelled()) {
                                                                    log.info("cancel receive device[{}] message[{}] reply", operator.getDeviceId(), msg.getMessageId());
                                                                }
                                                            }
                                                        })
                                                        .subscribe(replySink::next);
                                                replySink.onDispose(disposable);
                                                sink.next(msg);
                                                sink.complete();
                                            })
                                            .doOnNext(msg -> log.debug("send device[{}] message to server[{}] : {}", operator.getDeviceId(), serverId, msg))
                                            .as(flux -> handler.send(serverId, flux)))
                            .switchIfEmpty(Mono.just(-1))
                            .flatMap(len -> {
                                //设备未连接到服务器
                                if (len == 0) {
                                    return operator
                                            .checkState()
                                            .then(Mono.error(new DeviceOperationException(ErrorCode.CLIENT_OFFLINE)));
                                }
                                if (len == -1) {
                                    return Mono.error(new DeviceOperationException(ErrorCode.CLIENT_OFFLINE));
                                }
                                log.debug("send device[{}] message complete", operator.getDeviceId());
                                return Mono.just(true);
                            })
                            .doOnError(replySink::error)
                            .subscriberContext(replySink.currentContext())
                            .subscribe()
            );
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
