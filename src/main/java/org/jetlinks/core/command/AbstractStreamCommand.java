package org.jetlinks.core.command;

import org.reactivestreams.Publisher;
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

    public Self with(@Nonnull Flux<E> stream) {
        withStream(stream);
        return castSelf();
    }

    @Override
    @SuppressWarnings("all")
    public Self with(Object parameterObject) {
        if (parameterObject instanceof Publisher) {
            withStream(Flux.from((Publisher) parameterObject));
            return castSelf();
        }
        return super.with(parameterObject);
    }

    @Override
    @SuppressWarnings("all")
    public Self with(Command<?> command) {
        if (command.isWrapperFor(StreamCommand.class)) {
            this.stream = command
                .unwrap(StreamCommand.class)
                .stream()
                .map(this::convertStreamValue);
        }
        return super.with(command);
    }
}
