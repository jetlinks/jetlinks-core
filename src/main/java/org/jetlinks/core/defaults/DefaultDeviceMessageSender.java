package org.jetlinks.core.defaults;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.*;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.reactivestreams.Publisher;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

@Slf4j
public class DefaultDeviceMessageSender implements DeviceMessageSender {

    private final DeviceOperationBroker handler;

    private final DeviceOperator operator;

    private final DeviceRegistry registry;

    @Setter
    @Getter
    private long defaultTimeout = TimeUnit.SECONDS.toMillis(10);

    private final DeviceMessageSenderInterceptor globalInterceptor;

    public DefaultDeviceMessageSender(DeviceOperationBroker handler,
                                      DeviceOperator operator,
                                      DeviceRegistry registry,
                                      DeviceMessageSenderInterceptor interceptor) {
        this.handler = handler;
        this.operator = operator;
        this.registry = registry;
        this.globalInterceptor = interceptor;
    }

    @Override
    public <R extends DeviceMessageReply> Flux<R> send(Publisher<RepayableDeviceMessage<R>> message) {
        return send(message, this::convertReply);
    }

    protected <T extends DeviceMessageReply> T convertReply(Message sent, Object reply) {
        if (reply instanceof ChildDeviceMessageReply) {
            if (!(sent instanceof ChildDeviceMessage)) {
                ChildDeviceMessageReply messageReply = ((ChildDeviceMessageReply) reply);
                if (!messageReply.isSuccess()) {
                    //如果是可识别的错误则直接抛出异常
                    ErrorCode.of(messageReply.getCode())
                            .map(DeviceOperationException::new)
                            .ifPresent(err -> {
                                throw err;
                            });
                }
                if (messageReply.getChildDeviceMessage() == null) {
                    ErrorCode.of(messageReply.getCode())
                            .map(DeviceOperationException::new)
                            .ifPresent(err -> {
                                throw err;
                            });
                    throw new DeviceOperationException(ErrorCode.NO_REPLY);
                }
                return convertReply(((ChildDeviceMessageReply) reply).getChildDeviceMessage());
            }
        }
        return convertReply(reply);
    }


    protected <T extends DeviceMessageReply> T convertReply(Object obj) {
        if (obj instanceof DeviceMessageReply) {
            DeviceMessageReply reply = ((DeviceMessageReply) obj);
            if (!reply.isSuccess()) {
                //如果是可识别的错误则直接抛出异常
                ErrorCode.of(reply.getCode())
                        .map(DeviceOperationException::new)
                        .ifPresent(err -> {
                            throw err;
                        });
            }
            return (T) reply;
        }
        throw new DeviceOperationException(ErrorCode.SYSTEM_ERROR, new ClassCastException("can not cast " + obj.getClass() + " to DeviceMessageReply"));
    }

    private <R extends DeviceMessage> Flux<R> logReply(DeviceMessage msg, Flux<R> flux) {
        if (log.isDebugEnabled()) {
            return flux.doOnNext(r -> log.debug("receive device[{}] message[{}]: {}", operator.getDeviceId(), r.getMessageId(), r))
                    .doOnComplete(() -> log.debug("complete receive device[{}] message[{}]", operator.getDeviceId(), msg.getMessageId()))
                    .doOnCancel(() -> log.debug("cancel receive device[{}] message[{}]", operator.getDeviceId(), msg.getMessageId()));
        }
        return flux;
    }

    public <R extends DeviceMessage> Flux<R> send(Publisher<? extends DeviceMessage> message, Function<Object, R> replyMapping) {
        return Mono.zip(
                operator.getConnectionServerId().defaultIfEmpty(""), //当前设备连接的服务器ID
                operator.getProtocol()
                        .flatMap(ProtocolSupport::getSenderInterceptor)     //拦截器
                        .defaultIfEmpty(DeviceMessageSenderInterceptor.DO_NOTING),
                operator.getSelfConfig(DeviceConfigKey.parentGatewayId).defaultIfEmpty("")
        ).flatMapMany(serverAndInterceptor -> {

            DeviceMessageSenderInterceptor interceptor = serverAndInterceptor.getT2().andThen(globalInterceptor);
            String server = serverAndInterceptor.getT1();
            String parentGatewayId = serverAndInterceptor.getT3();
            //设备未连接,有上级网关设备则通过父级设备发送消息
            if (StringUtils.isEmpty(server) && StringUtils.hasText(parentGatewayId)) {
                return Flux
                        .from(message)
                        .flatMap(msg -> interceptor.preSend(operator, msg))
                        .flatMap(msg -> {
                            ChildDeviceMessage children = new ChildDeviceMessage();
                            children.setDeviceId(parentGatewayId);
                            children.setMessageId(msg.getMessageId());
                            children.setTimestamp(msg.getTimestamp());
                            children.setChildDeviceId(operator.getDeviceId());
                            children.setChildDeviceMessage(msg);

                            // https://github.com/jetlinks/jetlinks-pro/issues/19
                            children.setHeaders(msg.getHeaders());

                            return registry
                                    .getDevice(parentGatewayId)
                                    .switchIfEmpty(Mono.error(() -> new DeviceOperationException(ErrorCode.UNKNOWN_PARENT_DEVICE, "未知的父设备:" + parentGatewayId)))
                                    .flatMapMany(parent -> parent
                                            .messageSender()
                                            .send(Mono.just(children), resp -> this.convertReply(msg, resp)))
                                    .map(r -> (R) r)
                                    .as(flux -> interceptor.afterSent(operator, msg, flux));
                        });
            }
            return Flux.from(message)
                    .flatMap(msg -> interceptor.preSend(operator, msg))
                    .concatMap(msg -> {
                        if (StringUtils.isEmpty(server)) {
                            return interceptor.afterSent(operator, msg, Flux.error(new DeviceOperationException(ErrorCode.CLIENT_OFFLINE)));
                        }
                        //处理来自设备的回复.
                        Flux<R> replyStream = handler
                                .handleReply(msg.getMessageId(), Duration.ofMillis(msg.getHeader(Headers.timeout).orElse(defaultTimeout)))
                                .map(replyMapping)
                                .onErrorResume(DeviceOperationException.class, error -> {
                                    if (error.getCode() == ErrorCode.CLIENT_OFFLINE) {
                                        return operator
                                                .checkState()
                                                .then(Mono.error(error));
                                    }
                                    return Mono.error(error);
                                })
                                .onErrorMap(TimeoutException.class, timeout -> new DeviceOperationException(ErrorCode.TIME_OUT, timeout))
                                .as(flux -> this.logReply(msg, flux));
                        //发送消息到设备连接的服务器
                        return handler
                                .send(server, Mono.just(msg))
                                .defaultIfEmpty(-1)
                                .flatMap(len -> {
                                    //设备未连接到服务器
                                    if (len == 0) {
                                        //尝试发起状态检查,同步设备的真实状态
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
