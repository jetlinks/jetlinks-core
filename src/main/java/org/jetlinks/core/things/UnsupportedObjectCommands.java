package org.jetlinks.core.things;

import org.jetlinks.core.command.Command;
import org.jetlinks.core.command.CommandException;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.metadata.FunctionMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/** Shared unsupported handle; reactive entry points fail on subscription. */
final class UnsupportedObjectCommands implements CommandSupport {
    static final CommandSupport INSTANCE = new UnsupportedObjectCommands();

    private UnsupportedObjectCommands() {
    }

    @Override
    public <R> R execute(@Nonnull Command<R> command) {
        throw unsupported(command);
    }

    @Override
    public Flux<Object> executeToFlux(Command<?> command) {
        return Flux.error(() -> unsupported(command));
    }

    @Override
    public Mono<Object> executeToMono(Command<?> command) {
        return Mono.error(() -> unsupported(command));
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(String commandId) {
        return Mono.empty();
    }

    private CommandException unsupported(Command<?> command) {
        return new CommandException.NoStackTrace(this, command, "error.unsupported_command", null, command.getCommandId());
    }
}
