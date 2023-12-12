package org.jetlinks.core.utils;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@AllArgsConstructor(staticName = "of")
public class NamedFunction<T,R> implements Function<T,R> {

    private final String name;

    private final Function<T,R> call;

    @Override
    public R apply(T t) {
        return call.apply(t);
    }

    @Override
    public String toString() {
        return name;
    }
}
