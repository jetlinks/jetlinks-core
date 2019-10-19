package org.jetlinks.core.device;

import lombok.Setter;
import org.jetlinks.core.message.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StandaloneDeviceMessageHandler implements DeviceMessageHandler {

    private Map<String, Consumer<Message>> messageHandlers = new ConcurrentHashMap<>();

    private Map<String, EmitterProcessor<DeviceMessageReply>> replyProcessor = new ConcurrentHashMap<>();

    private Map<String, AtomicInteger> partCache = new ConcurrentHashMap<>();

    @Setter
    private ReplyFailureHandler handler;

    private Function<String, Byte> deviceStateGetter;

    public StandaloneDeviceMessageHandler(Function<String, Byte> deviceStateGetter) {
        this.deviceStateGetter = deviceStateGetter;
    }

    @Override
    public void handleDeviceMessage(String serverId, Consumer<Message> deviceMessageConsumer) {
        messageHandlers.put(serverId, deviceMessageConsumer);
    }

    @Override
    public Mono<Map<String, Byte>> getDeviceState(String serviceId, Collection<String> deviceIdList) {
        return Flux.fromIterable(deviceIdList)
                .map(id -> Tuples.of(id, deviceStateGetter.apply(id)))
                .collect(Collectors.toMap(Tuple2::getT1, Tuple2::getT2));
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
                messageHandlers.remove(messageId);
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

        return Mono.justOrEmpty(messageHandlers.get(serverId))
                .flatMap(c -> Flux.from(message)
                        .doOnNext(c)
                        .then(Mono.just(1)))
                .defaultIfEmpty(0);

    }

    @Override
    public Mono<Integer> send(Publisher<? extends BroadcastMessage> message) {
        // TODO: 2019-10-19 发送广播消息
        return Mono.just(0);
    }
}
