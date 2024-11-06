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
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.jetlinks.core.device.DeviceConfigKey.connectionServerId;

@Slf4j
public class DefaultDeviceMessageSender implements DeviceMessageSender {

    //设备操作代理,用于管理集群间设备指令发送
    private final DeviceOperationBroker handler;

    //设备操作接口,用于发送指令到设备,以及获取配置等相关信息
    private final DeviceOperator operator;

    //设备注册中心,用于统一管理设备以及产品的基本信息,缓存,进行设备指令下发等操作
    private final DeviceRegistry registry;

    private static final long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(Integer.getInteger("jetlinks.device.message.default-timeout", 10));

    @Setter
    @Getter
    private long defaultTimeout = DEFAULT_TIMEOUT;

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

    /**
     * 发送一个支持回复的消息.
     * <p>
     * ⚠️: 请勿自己实现消息对象,而应该使用框架定义的3种消息.
     * ⚠️: 如果是异步消息,将直接返回<code>{"success":true,"code":"REQUEST_HANDLING"}</code>
     *
     * @param message 具体的消息对象
     * @param <R>     返回类型
     * @return 异步发送结果
     * @see org.jetlinks.core.message.property.ReadPropertyMessage
     * @see org.jetlinks.core.message.property.ReadPropertyMessageReply
     * @see org.jetlinks.core.message.property.WritePropertyMessage
     * @see org.jetlinks.core.message.property.WritePropertyMessageReply
     * @see org.jetlinks.core.message.function.FunctionInvokeMessage
     * @see org.jetlinks.core.message.function.FunctionInvokeMessageReply
     * @see org.jetlinks.core.enums.ErrorCode#CLIENT_OFFLINE
     * @see org.jetlinks.core.enums.ErrorCode#REQUEST_HANDLING
     * @see org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor
     */
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

    @SuppressWarnings("all")
    protected <T extends DeviceMessage> T convertReply(Object obj) {
        DeviceMessage result = null;
        if (obj instanceof DeviceMessageReply) {
            DeviceMessageReply reply = ((DeviceMessageReply) obj);
            if (!reply.isSuccess()) {
                //如果是可识别的错误则直接抛出异常
                ErrorCode
                    .of(reply.getCode())
                    .map(code -> {
                        String msg = reply
                            .getHeader("errorMessage")
                            .map(String::valueOf)
                            .orElse(code.getText());
                        return new DeviceOperationException(code, msg);
                    })
                    .ifPresent(err -> {
                        throw err;
                    });
            }
            result = reply;
        } else if (obj instanceof DeviceMessage) {
            result = (DeviceMessage) obj;
        } else if (obj instanceof Map) {
            result = (DeviceMessage) MessageType.convertMessage(((Map) obj)).orElse(null);
        }
        if (result == null) {
            throw new DeviceOperationException.NoStackTrace(
                ErrorCode.SYSTEM_ERROR,
                new ClassCastException("can not cast " + obj + " to DeviceMessageReply"));
        }
        return (T) result;
    }

    private <R extends DeviceMessage> Flux<R> logReply(DeviceMessage msg, Flux<R> flux) {
        if (log.isDebugEnabled()) {
            return flux
                .doOnNext(r -> log.debug(
                    "receive device[{}] message[{}]: {}",
                    operator.getDeviceId(),
                    r.getMessageId(), r))

                .doOnComplete(() -> log.debug(
                    "complete receive device[{}] message[{}]",
                    operator.getDeviceId(),
                    msg.getMessageId()))

                .doOnCancel(() -> log.debug(
                    "cancel receive device[{}] message[{}]",
                    operator.getDeviceId(),
                    msg.getMessageId()));
        }
        return flux;
    }

    /**
     * 发送消息并获取返回
     *
     * @param message 消息
     * @param <R>     回复类型
     * @return 异步发送结果
     * @see DeviceMessageSender#send(Publisher)
     */
    @Override
    public <R extends DeviceMessage> Flux<R> send(DeviceMessage message) {
        return send(Mono.just(message), this::convertReply);
    }

    private Mono<String> refreshAndGetConnectionServerId() {
        return Mono
            .defer(() -> operator
                .refreshConfig(Collections.singleton(connectionServerId.getKey()))
                .then(operator.getConnectionServerId()));
    }

    private ChildDeviceMessage createChildDeviceMessage(String parentId, DeviceMessage message) {
        ChildDeviceMessage children = new ChildDeviceMessage();
        children.setDeviceId(parentId);
        children.setMessageId(message.getMessageId());
        children.setTimestamp(message.getTimestamp());
        children.setChildDeviceId(operator.getDeviceId());
        children.setChildDeviceMessage(message);

        // https://github.com/jetlinks/jetlinks-pro/issues/19
        Headers.copyFunctionalHeader(message, children);
        message.addHeader(Headers.dispatchToParent, true);
        children.validate();
        return children;
    }

    private Flux<DeviceMessage> sendToParentDevice(String parentId,
                                                   DeviceMessage message) {
        if (parentId.equals(operator.getDeviceId())) {
            return Flux
                .error(
                    new DeviceOperationException.NoStackTrace(ErrorCode.CYCLIC_DEPENDENCE, "validation.parent_id_and_id_can_not_be_same")
                );
        }

        ChildDeviceMessage children = createChildDeviceMessage(parentId, message);
        return registry
            .getDevice(parentId)
            .switchIfEmpty(Mono.error(() -> new DeviceOperationException.NoStackTrace(ErrorCode.UNKNOWN_PARENT_DEVICE)))
            .flatMapMany(parent -> parent
                .messageSender()
                .send(Mono.just(children), resp -> this.convertReply(message, resp)))
            ;
    }

    /**
     * 发送消息并自定义返回结果转换器
     *
     * @param message      消息
     * @param replyMapping 消息回复转换器
     * @param <R>          回复类型
     * @return 异步发送结果
     * @see DeviceMessageSender#send(Publisher)
     */
    public <R extends DeviceMessage> Flux<R> send(Publisher<? extends DeviceMessage> message, Function<Object, R> replyMapping) {
        // FIXME: 2023/6/29 重构...
        return Mono
            .zip(
                //当前设备连接的服务器ID
                operator.getConnectionServerId()
                        .switchIfEmpty(refreshAndGetConnectionServerId())
                        .defaultIfEmpty(""),
                //拦截器
                operator.getProtocol()
                        .flatMap(ProtocolSupport::getSenderInterceptor)
                        .defaultIfEmpty(DeviceMessageSenderInterceptor.DO_NOTING),
                //网关id
                operator.getSelfConfig(DeviceConfigKey.parentGatewayId).defaultIfEmpty("")
            )
            .flatMapMany(serverAndInterceptor -> {

                DeviceMessageSenderInterceptor interceptor = serverAndInterceptor
                    .getT2()
                    .andThen(globalInterceptor);
                String server = serverAndInterceptor.getT1();
                String parentGatewayId = serverAndInterceptor.getT3();
                //有上级网关设备则通过父级设备发送消息
                if (!StringUtils.hasText(server) && StringUtils.hasText(parentGatewayId)) {
                    return Flux
                        .from(message)
                        .flatMap(msg -> interceptor.preSend(operator, msg))
                        .flatMap(msg -> this
                            .sendToParentDevice(parentGatewayId, msg)
                            .as(flux -> interceptor.afterSent(operator, msg, interceptor.doSend(operator, msg, flux)))
                        )
                        .map(r -> (R) r);
                }
                return Flux
                    .from(message)
                    .flatMap(msg -> interceptor.preSend(operator, msg))
                    .concatMap(msg -> Flux
                        .defer(() -> {
                            //缓存中没有serverId,说明当前设备并未连接到平台.
                            if (ObjectUtils.isEmpty(server)) {
                                return interceptor.afterSent(
                                    operator,
                                    msg,
                                    Flux.error(new DeviceOperationException.NoStackTrace(ErrorCode.CLIENT_OFFLINE)));
                            }
                            boolean forget = msg.getHeader(Headers.sendAndForget).orElse(false);
                            //定义处理来自设备的回复.
                            Flux<R> replyStream = forget
                                ? Flux.empty()
                                : handler
                                //监听来自其他服务的回复
                                .handleReply(msg, Duration.ZERO)
                                .map(replyMapping)
                                .onErrorResume(DeviceOperationException.class, error -> {
                                    if (error.getCode() == ErrorCode.CLIENT_OFFLINE) {
                                        //返回离线错误,重新检查状态,以矫正设备缓存的状态
                                        return operator
                                            .checkState()
                                            .then(Mono.error(error));
                                    }
                                    return Mono.error(error);
                                })
                                .as(flux -> this.logReply(msg, flux));

                            //发送消息到设备连接的服务器
                            return handler
                                .send(server, Mono.just(msg))
                                .defaultIfEmpty(-1)
                                .flatMapMany(len -> {
                                    //设备未连接到服务器
                                    if (len == 0) {
                                        //尝试发起状态检查,同步设备的真实状态
                                        return operator
                                            .checkState()
                                            .flatMapMany(state -> {
                                                if (DeviceState.online != state) {
                                                    return interceptor
                                                        .afterSent(operator,
                                                                   msg,
                                                                   Flux.error(new DeviceOperationException.NoStackTrace(ErrorCode.CLIENT_OFFLINE)));
                                                }
                                                        /*
                                                          设备在线,但是serverId对应的服务没有监听处理消息
                                                             1. 服务挂了
                                                             2. 设备缓存的serverId不对
                                                         */
                                                //尝试发送给父设备
                                                if (StringUtils.hasText(parentGatewayId)) {
                                                    log.debug("Device [{}] Cached Server [{}] Not Available,Dispatch To Parent [{}]",
                                                              operator.getDeviceId(),
                                                              server,
                                                              parentGatewayId);

                                                    return interceptor
                                                        .afterSent(operator, msg, sendToParentDevice(parentGatewayId, msg))
                                                        .map(r -> (R) r);
                                                }
                                                log.warn("Device [{}] Cached Server [{}] Not Available",
                                                         operator.getDeviceId(),
                                                         server);

                                                return interceptor.afterSent(operator, msg, Flux.error(new DeviceOperationException.NoStackTrace(ErrorCode.SERVER_NOT_AVAILABLE)));
                                            });
                                    } else if (len == -1) {
                                        return interceptor.afterSent(operator, msg, Flux.error(new DeviceOperationException.NoStackTrace(ErrorCode.CLIENT_OFFLINE)));
                                    }
                                    log.debug("send device[{}] message complete", operator.getDeviceId());
                                    return interceptor.afterSent(operator, msg, replyStream);
                                });
                        })
                        .timeout(Duration.ofMillis(msg.getHeader(Headers.timeout).orElse(defaultTimeout)),
                                 Mono.error(() -> new DeviceOperationException.NoStackTrace(ErrorCode.TIME_OUT)))
                        .onErrorMap(TimeoutException.class, timeout -> new DeviceOperationException.NoStackTrace(ErrorCode.TIME_OUT, timeout))
                        .as(flux -> interceptor
                            .doSend(operator, msg, flux.cast(DeviceMessage.class))
                            .map(_resp -> (R) _resp))

                    );
            });

    }

    /**
     * 发送{@link org.jetlinks.core.message.function.FunctionInvokeMessage}消息更便捷的API
     *
     * @param function 要执行的功能
     * @return FunctionInvokeMessageSender
     * @see DeviceMessageSender#send(Publisher)
     * @see org.jetlinks.core.message.function.FunctionInvokeMessage
     * @see FunctionInvokeMessageSender
     */
    @Override
    public FunctionInvokeMessageSender invokeFunction(String function) {
        return new DefaultFunctionInvokeMessageSender(operator, function);
    }

    /**
     * 发送{@link org.jetlinks.core.message.property.ReadPropertyMessage}消息更便捷的API
     *
     * @param property 要获取的属性列表
     * @return ReadPropertyMessageSender
     * @see DeviceMessageSender#send(Publisher)
     * @see org.jetlinks.core.message.property.ReadPropertyMessage
     * @see ReadPropertyMessageSender
     */
    @Override
    public ReadPropertyMessageSender readProperty(String... property) {
        return new DefaultReadPropertyMessageSender(operator)
            .read(property);
    }

    /**
     * 发送{@link org.jetlinks.core.message.property.WritePropertyMessage}消息更便捷的API
     *
     * @return WritePropertyMessageSender
     * @see DeviceMessageSender#send(Publisher)
     * @see org.jetlinks.core.message.property.WritePropertyMessage
     * @see WritePropertyMessageSender
     */
    @Override
    public WritePropertyMessageSender writeProperty() {
        return new DefaultWritePropertyMessageSender(operator);
    }
}
