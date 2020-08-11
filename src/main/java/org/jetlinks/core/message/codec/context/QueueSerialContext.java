package org.jetlinks.core.message.codec.context;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
class QueueSerialContext<IN, OUT> implements SerialContext<IN, OUT> {
    int maxSize = 256;

    EmitterProcessor<IN> inputProcessor = EmitterProcessor.create(false);

    FluxSink<IN> inputSink = inputProcessor.sink(FluxSink.OverflowStrategy.BUFFER);

    Queue<Tuple2<IN, Consumer<OUT>>> inputQueue = new ConcurrentLinkedQueue<>();

    AtomicReference<Consumer<OUT>> output = new AtomicReference<>();

    AtomicBoolean awaiting = new AtomicBoolean();

    @Override
    public Mono<OUT> inputAndAwait(IN in, Duration timeout) {
        return Mono
                .<OUT>create(sink -> {
                    if (inputQueue.size() >= maxSize) {
                        sink.error(new UnsupportedOperationException("out of serial queue"));
                        drain();
                        return;
                    }
                    Tuple2<IN, Consumer<OUT>> tp2 = Tuples.of(in, (sink::success));
                    inputQueue.add(tp2);
                    drain();
                    sink.onDispose(() -> {
                        inputQueue.remove(tp2);
                        //next
                        drain();
                    });
                })
                .timeout(timeout, Mono.error(TimeoutException::new));
    }

    private void drain() {
        if (awaiting.compareAndSet(false, true)) {
            Tuple2<IN, Consumer<OUT>> input = inputQueue.poll();
            if (input != null) {
                output.set(input.getT2());
                inputSink.next(input.getT1());
            }
        }
    }

    @Override
    public void output(OUT out) {
        Consumer<OUT> consumer = output.getAndSet(null);
        if (consumer != null) {
            consumer.accept(out);
            awaiting.set(false);
        }
        drain();
    }

    @Override
    public Flux<IN> listen() {
        return inputProcessor;
    }
}
