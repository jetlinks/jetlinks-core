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
import org.reactivestreams.Publisher;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.*;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Slf4j
public class StandaloneDeviceMessageBroker implements DeviceOperationBroker, MessageHandler {

    private final Sinks.Many<Message> messageEmitterProcessor;

    private final Map<String, Sinks.Many<DeviceMessageReply>> replyProcessor = new ConcurrentHashMap<>();

    private final Map<String, AtomicInteger> partCache = new ConcurrentHashMap<>();

    @Setter
    private ReplyFailureHandler replyFailureHandler = (error, message) -> StandaloneDeviceMessageBroker.log.warn("unhandled reply message:{}", message, error);

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
                replyFailureHandler.handle(new NullPointerException("no reply handler " + result.name()), message);
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
        if (messageEmitterProcessor.currentSubscriberCount() == 0) {
            return Mono.just(0);
        }

        return Flux.from(message)
                   .doOnNext(messageEmitterProcessor::tryEmitNext)
                   .then(Mono.just(Long.valueOf(messageEmitterProcessor.currentSubscriberCount()).intValue()));
    }

    @Override
    public Mono<Integer> send(Publisher<? extends BroadcastMessage> message) {
        // TODO: 2019-10-19 发送广播消息
        return Mono.just(0);
    }


}
