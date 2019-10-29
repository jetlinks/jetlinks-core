package org.jetlinks.core.utils;

import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class FluxUtils {

    public static <T> Flux<List<T>> bufferRate(Flux<T> flux, int rate, Duration maxTimeout) {
        return bufferRate(flux, rate, 100, maxTimeout);
    }

    public static <T> Flux<List<T>> bufferRate(Flux<T> flux, int rate, int maxSize, Duration maxTimeout) {

        return Flux.create(sink -> sink.onDispose(Flux.<T>create(
                buffer ->
                        buffer.onDispose(
                                flux.elapsed()
                                        .subscribe(e -> {
                                            if (e.getT1() > rate) {
                                                sink.next(Collections.singletonList(e.getT2()));
                                            } else {
                                                buffer.next(e.getT2());
                                            }
                                        })))
                .bufferTimeout(maxSize, maxTimeout)
                .subscribe(sink::next)
        )
        );
    }

}
