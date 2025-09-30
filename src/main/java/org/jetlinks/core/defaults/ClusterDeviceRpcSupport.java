package org.jetlinks.core.defaults;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceState;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.server.session.DeviceSessionSelector;
import org.jetlinks.core.things.ThingRpcSupport;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

@Slf4j
class ClusterDeviceRpcSupport implements ThingRpcSupport, Function<Object, DeviceMessage> {

    private final DefaultDeviceOperator device;

    ClusterDeviceRpcSupport(DefaultDeviceOperator device) {
        this.device = device;
    }

    Flux<DeviceMessage> call(DeviceMessage message) {
        return Mono
            .zip(
                // 当前连接的服务ID
                device.getConnectionServerId().defaultIfEmpty(""),
                // 协议包定义的拦截器
                device.getProtocol()
                      .flatMap(ProtocolSupport::getSenderInterceptor)
                      .map(it -> it.andThen(device.interceptor))
                      .defaultIfEmpty(device.interceptor),
                //网关id
                device.getSelfConfig(DeviceConfigKey.parentGatewayId).defaultIfEmpty("")
            )
            .flatMapMany(tp3 -> tp3
                .getT2()
                // 执行拦截器
                .preSend(device, message)
                .flatMapMany(msg -> this.call(msg, tp3.getT1(), tp3.getT2(), tp3.getT3())));
    }

    private Flux<? extends DeviceMessage> call(
        DeviceMessage message,
        String serverId,
        DeviceMessageSenderInterceptor interceptor,
        String parentId) {

        if (message.getHeaderOrDefault(Headers.sessionSelector) == DeviceSessionSelector.any) {
            return doSend(message, serverId, interceptor, parentId);
        }
        // 指定了选择器时,传入空serverId,让broker去控制
        return doSend(message, null, interceptor, parentId);

    }

    private Flux<? extends DeviceMessage> doSend(DeviceMessage message,
                                                 String serverId,
                                                 DeviceMessageSenderInterceptor interceptor,
                                                 String parentId) {

        // 监听回复
        Flux<? extends DeviceMessage> await =
            message.getHeaderOrDefault(Headers.sendAndForget) ?
                Flux.empty() :
                device
                    .broker
                    .handleReply(message, Duration.ZERO)
                    .map(this)
                    .onErrorResume(
                        DeviceOperationException.class,
                        error -> {
                            if (error.getCode() == ErrorCode.CLIENT_OFFLINE) {
                                //返回离线错误,重新检查状态,以矫正设备缓存的状态
                                return device
                                    .checkState()
                                    .then(Mono.error(error));
                            }
                            return Mono.error(error);
                        });

        // 构造发送逻辑
        Flux<DeviceMessage> sender = device
            .broker
            .send(serverId, message)
            .defaultIfEmpty(0)
            .flatMapMany(received -> {
                // 没有被任何服务处理?
                if (received == 0) {
                    return interceptor.afterSent(device, message, handleNoHandler(serverId, message, parentId));
                }
                return interceptor.afterSent(device, message, await);
            })
            .timeout(Duration.ofMillis(message.getHeaderOrDefault(Headers.timeout)),
                     Mono.error(() -> new DeviceOperationException.NoStackTrace(ErrorCode.TIME_OUT)))
            .onErrorMap(TimeoutException.class, timeout -> new DeviceOperationException.NoStackTrace(ErrorCode.TIME_OUT, timeout));

        // 执行拦截器
        return interceptor.doSend(device, message, sender);

    }


    private Flux<DeviceMessage> handleNoHandler(String serverId, DeviceMessage message, String parentId) {
        return device
            .checkState()
            .flatMapMany(state -> {
                if (DeviceState.online != state) {
                    return Flux.error(new DeviceOperationException.NoStackTrace(ErrorCode.CLIENT_OFFLINE));
                }
                //尝试发送给父设备
                if (StringUtils.hasText(parentId)) {
                    log.debug("Device [{}] Cached Server [{}] Not Available,Dispatch To Parent [{}]",
                              device.getDeviceId(),
                              serverId,
                              parentId);
                    return sendToParentDevice(parentId, message);
                }
                log.warn("Device [{}] Cached Server [{}] Not Available",
                         device.getDeviceId(),
                         serverId);

                return Flux.error(new DeviceOperationException.NoStackTrace(ErrorCode.SERVER_NOT_AVAILABLE));
            });
    }

    private Flux<DeviceMessage> sendToParentDevice(String parentId,
                                                   DeviceMessage message) {
        if (parentId.equals(device.getDeviceId())) {
            return Flux
                .error(
                    new DeviceOperationException.NoStackTrace(ErrorCode.CYCLIC_DEPENDENCE, "validation.parent_id_and_id_can_not_be_same")
                );
        }

        ChildDeviceMessage children = createChildDeviceMessage(parentId, message);
        return device
            .registry
            .getDevice(parentId)
            .switchIfEmpty(Mono.error(() -> new DeviceOperationException.NoStackTrace(ErrorCode.UNKNOWN_PARENT_DEVICE)))
            .flatMapMany(parent -> parent
                .rpc()
                .call(children)
                .map(reply -> convertReply(message, reply))
            )
            ;
    }


    private ChildDeviceMessage createChildDeviceMessage(String parentId, DeviceMessage message) {
        ChildDeviceMessage children = new ChildDeviceMessage();
        children.setDeviceId(parentId);
        children.setMessageId(message.getMessageId());
        children.setTimestamp(message.getTimestamp());
        children.setChildDeviceId(device.getDeviceId());
        children.setChildDeviceMessage(message);

        // https://github.com/jetlinks/jetlinks-pro/issues/19
        Headers.copyFunctionalHeader(message, children);
        message.addHeader(Headers.dispatchToParent, true);
        children.validate();
        return children;
    }


    @Override
    public Flux<? extends ThingMessage> call(ThingMessage message) {
        if(!StringUtils.hasText(message.getMessageId())){
            message.messageId(IDGenerator.RANDOM.generate());
        }
        if (message instanceof DeviceMessage) {
            return call(((DeviceMessage) message));
        }

        return call(convertToDeviceMessage(message));
    }


    private DeviceMessage convertToDeviceMessage(ThingMessage message) {
        if (message instanceof DeviceMessage) {
            return ((DeviceMessage) message);
        }
        //将非DeviceMessage转为DeviceMessage

        JSONObject msg = message.toJson();
        msg.remove("thingId");
        msg.remove("thingType");
        msg.put("deviceId", message.getThingId());
        return MessageType
            .convertMessage(msg)
            .filter(DeviceMessage.class::isInstance)
            .map(DeviceMessage.class::cast)
            .orElseThrow(() -> new UnsupportedOperationException("unsupported message type " + message.getMessageType()));
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


    @Override
    public DeviceMessage apply(Object o) {
        return convertReply(o);
    }
}
