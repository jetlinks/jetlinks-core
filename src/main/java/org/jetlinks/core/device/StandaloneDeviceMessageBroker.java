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
import reactor.core.publisher.*;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Slf4j
public class StandaloneDeviceMessageBroker implements DeviceOperationBroker, MessageHandler {

    private final FluxProcessor<Message, Message> messageEmitterProcessor;


    private final FluxSink<Message> sink;

    private final Map<String, FluxProcessor<DeviceMessageReply, DeviceMessageReply>> replyProcessor = new ConcurrentHashMap<>();

    private final Map<String, AtomicInteger> partCache = new ConcurrentHashMap<>();

    @Setter
    private ReplyFailureHandler replyFailureHandler = (error, message) -> log.warn("unhandled reply message:{}", message, error);

    private final Map<String, Function<Publisher<String>, Flux<DeviceStateInfo>>> stateHandler = new ConcurrentHashMap<>();

    public StandaloneDeviceMessageBroker() {
        this(EmitterProcessor.create(false));

    }

    public StandaloneDeviceMessageBroker(FluxProcessor<Message, Message> processor) {
        this.messageEmitterProcessor = processor;
        this.sink=processor.sink(FluxSink.OverflowStrategy.BUFFER);
    }

    @Override
    public Flux<Message> handleSendToDeviceMessage(String serverId) {
        return messageEmitterProcessor
                .map(Function.identity());
    }

    @Override
    public void handleGetDeviceState(String serverId, Function<Publisher<String>, Flux<DeviceStateInfo>> stateMapper) {
        stateHandler.put(serverId, stateMapper);
    }

    @Override
    public Flux<DeviceStateInfo> getDeviceState(String serviceId, Collection<String> deviceIdList) {
        return Mono.justOrEmpty(stateHandler.get(serviceId))
                .flatMapMany(fun -> fun.apply(Flux.fromIterable(deviceIdList)));
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
                FluxProcessor<DeviceMessageReply, DeviceMessageReply> processor = replyProcessor.getOrDefault(partMsgId, replyProcessor.get(messageId));

                if (processor == null || processor.isDisposed()) {
                    replyFailureHandler.handle(new NullPointerException("no reply handler"), message);
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
            FluxProcessor<DeviceMessageReply, DeviceMessageReply> processor = replyProcessor.get(messageId);

            if (processor != null && !processor.isDisposed()) {
                processor.onNext(message);
                processor.onComplete();
            } else {
                replyProcessor.remove(messageId);
                replyFailureHandler.handle(new NullPointerException("no reply handler"), message);
                return Mono.just(false);
            }
            return Mono.just(true);
        }).doOnError(err -> replyFailureHandler.handle(err, message));
    }

    @Override
    public Flux<DeviceMessageReply> handleReply(String messageId, Duration timeout) {

        return replyProcessor
                .computeIfAbsent(messageId, ignore -> UnicastProcessor.create())
                .timeout(timeout, Mono.error(() -> new DeviceOperationException(ErrorCode.TIME_OUT)))
                .doFinally(signal -> replyProcessor.remove(messageId));
    }

    @Override
    public Mono<Integer> send(String serverId, Publisher<? extends Message> message) {
        if (!messageEmitterProcessor.hasDownstreams()) {
            return Mono.just(0);
        }

        return Flux.from(message)
                .doOnNext(sink::next)
                .then(Mono.just(Long.valueOf(messageEmitterProcessor.downstreamCount()).intValue()));
    }

    @Override
    public Mono<Integer> send(Publisher<? extends BroadcastMessage> message) {
        // TODO: 2019-10-19 发送广播消息
        return Mono.just(0);
    }


}
