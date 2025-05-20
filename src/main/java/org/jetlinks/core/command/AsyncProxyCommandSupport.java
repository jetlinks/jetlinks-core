package org.jetlinks.core.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetlinks.core.metadata.FunctionMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

@AllArgsConstructor
public class AsyncProxyCommandSupport implements CommandSupport {

    private final Mono<? extends CommandSupport> asyncCommand;

    @Nonnull
    @Override
    @SuppressWarnings("all")
    public <R> R execute(@Nonnull Command<R> command) {
        //ProxyCommand 固定返回Flux
        if (command instanceof ProxyCommand) {
            return (R) executeToFlux(command);
        }

        if (CommandUtils.commandResponseMono(command)) {
            return (R) executeToMono(command);
        }

        return (R) executeToFlux(command);
    }

    @Override
    public final Flux<Object> executeToFlux(Command<?> command) {

        if (command instanceof ProxyCommand) {
            ProxyCommand cmd = ((ProxyCommand) command);
            return asyncCommand
                .flatMapMany(support -> support
                    .createCommandAsync(cmd.getCommandId())
                    .doOnNext(proxyCmd -> proxyCmd.with(cmd))
                    .flatMapMany(support::executeToFlux));
        }

        return asyncCommand
            .flatMapMany(support -> support.executeToFlux(command));
    }

    @Override
    public final Mono<Object> executeToMono(Command<?> command) {

        if (command instanceof ProxyCommand) {
            ProxyCommand cmd = ((ProxyCommand) command);
            return asyncCommand
                .flatMap(support -> support
                    .createCommandAsync(cmd.getCommandId())
                    .doOnNext(proxyCmd -> proxyCmd.with(cmd))
                    .flatMap(support::executeToMono));
        }

        return asyncCommand
            .flatMap(support -> support.executeToMono(command));
    }

    @Override
    @SuppressWarnings("all")
    public final <R, C extends Command<R>> C createCommand(String commandId) {
        return (C) ProxyCommand.of(commandId);
    }

    @Override
    public final <R, C extends Command<R>> Mono<C> createCommandAsync(String commandId) {
        return asyncCommand.flatMap(s -> s.<R, C>createCommandAsync(commandId));
    }

    @Override
    public final Flux<FunctionMetadata> getCommandMetadata() {
        return asyncCommand.flatMapMany(CommandSupport::getCommandMetadata);
    }

    @Override
    public final Mono<FunctionMetadata> getCommandMetadata(String commandId) {
        return asyncCommand.flatMap(s -> s.getCommandMetadata(commandId));
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(Command<?> command) {
        return asyncCommand.flatMap(s -> s.getCommandMetadata(command));
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(@Nonnull String commandId, @Nullable Map<String, Object> parameters) {
        return asyncCommand.flatMap(s -> s.getCommandMetadata(commandId, parameters));
    }

    @Override
    public Mono<Boolean> commandIsSupported(String commandId) {
        return asyncCommand.flatMap(s -> s.commandIsSupported(commandId));
    }

    @Override
    public Mono<Boolean> commandIsSupported(Command<?> cmd) {
        return asyncCommand.flatMap(s -> s.commandIsSupported(cmd));
    }

    @Override
    public Mono<Boolean> commandIsSupported(Class<? extends Command<?>> cmd) {
        return asyncCommand.flatMap(s -> s.commandIsSupported(cmd));
    }

    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    @Getter
    public static class ProxyCommand extends AbstractCommand<Object, ProxyCommand> {
        private String commandId;

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeUTF(commandId);
            super.writeExternal(out);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            commandId = in.readUTF();
            super.readExternal(in);
        }
    }
}
