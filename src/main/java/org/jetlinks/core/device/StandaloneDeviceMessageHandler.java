package org.jetlinks.core.device;

import lombok.Setter;
import org.jetlinks.core.message.BroadcastMessage;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class StandaloneDeviceMessageHandler implements DeviceMessageHandler {

    private EmitterProcessor<Message> messageEmitterProcessor = EmitterProcessor.create();

    private Map<String, EmitterProcessor<DeviceMessageReply>> replyProcessor = new ConcurrentHashMap<>();

    private Map<String, AtomicInteger> partCache = new ConcurrentHashMap<>();

    @Setter
    private ReplyFailureHandler handler;

    private Map<String, Function<Publisher<String>, Flux<DeviceStateInfo>>> stateHandler = new ConcurrentHashMap<>();


    @Override
    public Flux<Message> handleDeviceMessage(String serverId) {
        return messageEmitterProcessor
                .map(Function.identity());
    }

    @Override
    public void handleGetDeviceState(String serverId, Function<Publisher<String>, Flux<DeviceStateInfo>> stateMapper) {
        stateHandler.put(serverId, stateMapper);
    }

    @Override
    public Flux<DeviceStateInfo> getDeviceState(String serviceId, Publisher<String> deviceIdList) {
        return Mono.justOrEmpty(stateHandler.get(serviceId))
                .flatMapMany(fun -> fun.apply(deviceIdList));
    }

    @Override
    public Mono<Boolean> reply(DeviceMessageReply message) {
        return Mono.defer(() -> {

            String messageId = message.getMessageId();

            String partMsgId = message.getHeader(Headers.fragmentBodyMessageId).orElse(null);
            if (partMsgId != null) {
                EmitterProcessor<DeviceMessageReply> processor = replyProcessor.getOrDefault(partMsgId, replyProcessor.get(messageId));

                if (processor == null || processor.isCancelled()) {
                    if (handler != null) {
                        handler.handle(message);
                    }
                    replyProcessor.remove(partMsgId);
                    return Mono.just(false);
                }
                int partTotal = message.getHeader(Headers.fragmentNumber).orElse(1);
                AtomicInteger counter = partCache.computeIfAbsent(partMsgId, ignore -> new AtomicInteger(partTotal));

                processor.onNext(message);
                if (counter.decrementAndGet() <= 0) {
                    processor.onComplete();
                    replyProcessor.remove(partMsgId);
                }
                return Mono.just(true);
            }
            EmitterProcessor<DeviceMessageReply> processor = replyProcessor.get(messageId);

            if (processor != null) {
                processor.onNext(message);
                processor.onComplete();
            }
            return Mono.just(true);
        });
    }

    @Override
    public Flux<DeviceMessageReply> handleReply(String messageId, Duration timeout) {

        return replyProcessor
                .computeIfAbsent(messageId, ignore -> EmitterProcessor.create(true))
                .timeout(timeout)
                .doFinally(signal -> replyProcessor.remove(messageId));
    }

    @Override
    public Mono<Integer> send(String serverId, Publisher<? extends Message> message) {
        if (!messageEmitterProcessor.hasDownstreams()) {
            return Mono.just(0);
        }

        return Flux.from(message)
                .doOnNext(messageEmitterProcessor::onNext)
                .then(Mono.just(Long.valueOf(messageEmitterProcessor.inners().count()).intValue()));
    }

    @Override
    public Mono<Integer> send(Publisher<? extends BroadcastMessage> message) {
        // TODO: 2019-10-19 发送广播消息
        return Mono.just(0);
    }
}
