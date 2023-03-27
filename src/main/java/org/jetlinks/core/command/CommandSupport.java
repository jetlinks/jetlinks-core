package org.jetlinks.core.command;

import org.jetlinks.core.Wrapper;
import org.jetlinks.core.metadata.FunctionMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Objects;

public interface CommandSupport extends Wrapper {

    @Nonnull
    <R> R execute(@Nonnull Command<R> command);

    default <R, C extends Command<R>> C createCommand(String commandId) {
        throw new UnsupportedOperationException("unsupported command:" + commandId);
    }

    default Flux<FunctionMetadata> getCommandMetadata() {
        return Flux.empty();
    }

    default Mono<FunctionMetadata> getCommandMetadata(String commandId) {
        return getCommandMetadata()
                .filter(cmd -> Objects.equals(cmd.getId(), commandId))
                .singleOrEmpty();
    }
}
