package org.jetlinks.core.command;

import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.utils.Reactors;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

class LambdaCommandSupport<R, T extends Command<R>> implements CommandSupport {
    private final Class<T> commandType;
    private final Supplier<T> commandBuilder;
    private final Function<T, R> commandInvoker;

    @SuppressWarnings("all")
    LambdaCommandSupport(Supplier<T> commandBuilder, Function<T, R> commandInvoker) {
        this.commandBuilder = commandBuilder;
        this.commandInvoker = commandInvoker;
        this.commandType = (Class<T>) commandBuilder.get().getClass();
    }

    @Nonnull
    @Override
    @SuppressWarnings("all")
    public <R> R execute(@Nonnull Command<R> command) {
        // 类型一致
        if(command.isWrapperFor(commandType)){
            return (R) commandInvoker.apply(command.unwrap(commandType));
        }
        T cmd = commandBuilder.get();
        if (!Objects.equals(cmd.getCommandId(), command.getCommandId())) {
            throw new CommandException(this, command, "error.unsupported_command");
        }
        cmd.with(command);
        return (R) commandInvoker.apply(cmd);
    }

    @Override
    @SuppressWarnings("all")
    public <R2, C extends Command<R2>> C createCommand(String commandId) {
        Command<?> cmd = commandBuilder.get();
        if (Objects.equals(commandId, cmd.getCommandId())) {
            return (C) cmd;
        }
        return CommandSupport.super.createCommand(commandId);
    }

    @Override
    public Mono<Boolean> commandIsSupported(String commandId) {
        return Objects.equals(commandBuilder.get().getCommandId(), commandId)
            ? Reactors.ALWAYS_TRUE
            : Reactors.ALWAYS_FALSE;
    }

    @Override
    public Flux<FunctionMetadata> getCommandMetadata() {
        return Flux.just(CommandMetadataResolver.resolve(ResolvableType.forClass(commandType)));
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
