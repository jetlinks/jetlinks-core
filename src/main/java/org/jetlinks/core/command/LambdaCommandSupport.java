package org.jetlinks.core.command;

import lombok.AllArgsConstructor;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor
class LambdaCommandSupport<R, T extends Command<R>> implements CommandSupport {
    private final Supplier<T> commandBuilder;

    private final Function<T, R> commandInvoker;

    @Nonnull
    @Override
    @SuppressWarnings("all")
    public <R> R execute(@Nonnull Command<R> command) {
        T cmd = commandBuilder.get();
        if (!Objects.equals(cmd.getCommandId(), command.getCommandId())) {
            throw new CommandException(this, command, "error.unsupported_command");
        }
        cmd.with(command);
        return (R) commandInvoker.apply(cmd);
    }

    @Override
    public Flux<FunctionMetadata> getCommandMetadata() {
        return Flux.just(CommandMetadataResolver.resolve(ResolvableType.forInstance(commandBuilder.get())));
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(String commandId) {
        Command<?> cmd = commandBuilder.get();
        if (Objects.equals(commandId, cmd.getCommandId())) {
            return Mono.empty();
        }
        return Mono.just(CommandMetadataResolver.resolve(ResolvableType.forInstance(cmd)));
    }

}
