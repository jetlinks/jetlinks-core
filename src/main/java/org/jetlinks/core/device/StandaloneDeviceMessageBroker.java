package org.jetlinks.core.device;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.BroadcastMessage;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.server.MessageHandler;
import org.jetlinks.core.utils.Reactors;
import org.reactivestreams.Publisher;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.*;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Slf4j
public class StandaloneDeviceMessageBroker implements DeviceOperationBroker, MessageHandler {

    private final Sinks.Many<Message> messageEmitterProcessor;

    private final Map<String, Sinks.Many<DeviceMessageReply>> replyProcessor = new ConcurrentHashMap<>();

    private final Map<String, AtomicInteger> partCache = new ConcurrentHashMap<>();

    private final List<Function<Message, Mono<Void>>> handlers = new CopyOnWriteArrayList<>();

    @Setter
    private ReplyFailureHandler replyFailureHandler = (error, message) -> StandaloneDeviceMessageBroker.log.info("unhandled reply message:{}", message, error);

    private Function<Publisher<String>, Flux<DeviceStateInfo>> stateHandler;

    public StandaloneDeviceMessageBroker() {
        this(Sinks.many().multicast().onBackpressureBuffer());
    }

    public StandaloneDeviceMessageBroker(Sinks.Many<Message> processor) {
        this.messageEmitterProcessor = processor;
    }

    @Override
    public Flux<Message> handleSendToDeviceMessage(String serverId) {
        return messageEmitterProcessor.asFlux();
    }

    @Override
    public Disposable handleGetDeviceState(String serverId, Function<Publisher<String>, Flux<DeviceStateInfo>> stateMapper) {
        this.stateHandler = stateMapper;
        return () -> this.stateHandler = null;
    }

    @Override
    public Disposable handleSendToDeviceMessage(String serverId, Function<Message, Mono<Void>> handler) {
        handlers.add(handler);
        return () -> handlers.remove(handler);
    }

    @Override
    public Flux<DeviceStateInfo> getDeviceState(String serviceId, Collection<String> deviceIdList) {
        if (this.stateHandler != null) {
            return stateHandler.apply(Flux.fromIterable(deviceIdList));
        }
        return Flux.empty();
    }

    @Override
    public Mono<Boolean> reply(DeviceMessageReply message) {
        return Mono.defer(() -> {

            String messageId = message.getMessageId();
            if (StringUtils.isEmpty(messageId)) {
                log.warn("reply message messageId is empty: {}", message);
                return Mono.just(false);
            }

            String partMsgId = message.getHeader(Headers.fragmentBodyMessageId).orElse(null);
            if (partMsgId != null) {
                Sinks.Many<DeviceMessageReply> processor = replyProcessor.getOrDefault(partMsgId, replyProcessor.get(messageId));

                if (processor == null || processor.currentSubscriberCount() == 0) {
                    replyFailureHandler.handle(new NullPointerException("no reply handler"), message);
                    replyProcessor.remove(partMsgId);
                    return Mono.just(false);
                }
                int partTotal = message.getHeader(Headers.fragmentNumber).orElse(1);
                AtomicInteger counter = partCache.computeIfAbsent(partMsgId, ignore -> new AtomicInteger(partTotal));

                processor.emitNext(message, Sinks.EmitFailureHandler.FAIL_FAST);
                if (counter.decrementAndGet() <= 0) {
                    processor.tryEmitComplete();
                    replyProcessor.remove(partMsgId);
                }
                return Mono.just(true);
            }
            Sinks.Many<DeviceMessageReply> processor = replyProcessor.get(messageId);

            Sinks.EmitResult result = processor.tryEmitNext(message);
            if (result.isFailure()) {
                replyProcessor.remove(messageId);
                replyFailureHandler.handle(new DeviceOperationException.NoStackTrace(
                    ErrorCode.SYSTEM_ERROR,
                    "no reply handler " + result.name()), message);
                return Mono.just(false);
            }
            processor.tryEmitComplete();
            return Mono.just(true);
        }).doOnError(err -> replyFailureHandler.handle(err, message));
    }

    @Override
    public Flux<DeviceMessageReply> handleReply(String deviceId, String messageId, Duration timeout) {

        return replyProcessor
            .computeIfAbsent(messageId, ignore -> Sinks.many().multicast().onBackpressureBuffer())
            .asFlux()
            .as(flux -> {
                if (timeout.isZero()) {
                    return flux;
                }
                return flux.timeout(timeout, Mono.error(() -> new DeviceOperationException(ErrorCode.TIME_OUT)));
            })
            .doFinally(signal -> replyProcessor.remove(messageId));
    }

    @Override
    public Mono<Integer> send(String serverId, Publisher<? extends Message> message) {
        if (messageEmitterProcessor.currentSubscriberCount() == 0 && handlers.isEmpty()) {
            return Reactors.ALWAYS_ZERO;
        }

        return Flux
            .from(message)
            .flatMap(this::send)
            .then(Reactors.ALWAYS_ONE);
    }

    private Mono<Void> send(Message msg) {

        if (messageEmitterProcessor.currentSubscriberCount() > 0) {
            messageEmitterProcessor.emitNext(msg, Reactors.emitFailureHandler());
        }

        int size = handlers.size();

        if (size == 0) {
            return Mono.empty();
        }

        if (size == 1) {
            return handlers.get(0).apply(msg);
        }

        return Flux
            .fromIterable(handlers)
            .flatMap(handler -> handler.apply(msg))
            .then();
    }

    @Override
    public Mono<Integer> send(Publisher<? extends BroadcastMessage> message) {
        // TODO: 2019-10-19 发送广播消息
        return Mono.just(0);
    }


}
