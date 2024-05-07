package org.jetlinks.core.command;

import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;

public abstract class AbstractStreamCommand<E, R, Self extends AbstractStreamCommand<E, R, Self>>
    extends AbstractCommand<Flux<R>, Self> implements StreamCommand<E, R> {

    protected transient Flux<E> stream;

    @Nonnull
    @Override
    public Flux<E> stream() {
        return stream == null ? Flux.empty() : stream;
    }

    @Override
    public void withStream(@Nonnull Flux<E> stream) {
        this.stream = stream;
    }

}
