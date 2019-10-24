package org.jetlinks.core.utils;

import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FluxUtils {

    public static <T> Flux<List<T>> bufferRate(Flux<T> flux, int rate, Duration maxTimeout) {
        return Flux.create(sink -> sink.onDispose(flux.elapsed()
                .bufferWhile(r -> {
                    if (r.getT1() < rate) {
                        return true;
                    }
                    sink.next(Collections.singletonList(r.getT2()));

                    return false;
                })
                .map(list -> list.stream().map(Tuple2::getT2).collect(Collectors.toList()))
                .bufferTimeout(10, maxTimeout)
                .doOnError(sink::error)
                .doOnComplete(sink::complete)
                .flatMap(Flux::fromIterable)
                .subscribe(sink::next)));
    }

}
