package org.jetlinks.core.command;

import lombok.AllArgsConstructor;
import org.jetlinks.core.metadata.FunctionMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;

@AllArgsConstructor
public class ProxyCommandSupportAdapter implements ProxyCommandSupport {
    private final CommandSupport target;

    @Override
    public CommandSupport getProxyTarget() {
        return target;
    }

    @Nonnull
    @Override
    public <R> R execute(@Nonnull Command<R> command) {
        return target.execute(command);
    }

    @Override
    public Flux<Object> executeToFlux(Command<?> command) {
        return target.executeToFlux(command);
    }

    @Override
    public Flux<Object> executeToFlux(String commandId, Map<String, Object> parameters) {
        return target.executeToFlux(commandId, parameters);
    }

    @Override
    public Flux<Object> executeToFlux(String commandId, Map<String, Object> parameters, Flux<Object> stream) {
        return target.executeToFlux(commandId, parameters, stream);
    }

    @Override
    public Mono<Boolean> commandIsSupported(Command<?> cmd) {
        return target.commandIsSupported(cmd);
    }

    @Override
    public Mono<Boolean> commandIsSupported(String commandId) {
        return target.commandIsSupported(commandId);
    }

    @Override
    public <R, C extends Command<R>> C createCommand(String commandId) {
        return target.createCommand(commandId);
    }

    @Override
    public <R, C extends Command<R>> Mono<C> createCommandAsync(String commandId) {
        return target.createCommandAsync(commandId);
    }

    @Override
    public Mono<Boolean> commandIsSupported(Class<? extends Command<?>> cmd) {
        return target.commandIsSupported(cmd);
    }

    @Override
    public Flux<FunctionMetadata> getCommandMetadata() {
        return target.getCommandMetadata();
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(String commandId) {
        return target.getCommandMetadata(commandId);
    }

    @Override
    public Mono<Object> executeToMono(Command<?> command) {
        return target.executeToMono(command);
    }

    @Override
    public Mono<Object> executeToMono(String commandId, Map<String, Object> parameters) {
        return target.executeToMono(commandId, parameters);
    }

    @Override
    public boolean isWrapperFor(Class<?> type) {
        return ProxyCommandSupport.super.isWrapperFor(type);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return ProxyCommandSupport.super.unwrap(type);
    }
}
