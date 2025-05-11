package org.jetlinks.core.command;

import lombok.AllArgsConstructor;
import org.jetlinks.core.metadata.FunctionMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@AllArgsConstructor
public abstract class TemplateCommandSupport implements CommandSupport {

    protected final CommandSupport template;

    @Nonnull
    @Override
    public abstract <R> R execute(@Nonnull Command<R> command);

    @Override
    public <R, C extends Command<R>> C createCommand(String commandId) {
        return template.createCommand(commandId);
    }

    @Override
    public <R, C extends Command<R>> Mono<C> createCommandAsync(String commandId) {
        return template.createCommandAsync(commandId);
    }

    @Override
    public Flux<FunctionMetadata> getCommandMetadata() {
        return template.getCommandMetadata();
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(String commandId) {
        return template.getCommandMetadata(commandId);
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(@Nonnull String commandId, @Nullable Map<String, Object> parameters) {
        return template.getCommandMetadata(commandId, parameters);
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(Command<?> command) {
        return template.getCommandMetadata(command);
    }

    @Override
    public Mono<Boolean> commandIsSupported(Command<?> cmd) {
        return template.commandIsSupported(cmd);
    }

    @Override
    public Mono<Boolean> commandIsSupported(Class<? extends Command<?>> cmd) {
        return template.commandIsSupported(cmd);
    }

    @Override
    public Mono<Boolean> commandIsSupported(String commandId) {
        return template.commandIsSupported(commandId);
    }

    @Override
    public boolean isWrapperFor(Class<?> type) {
        return template.isWrapperFor(type);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return template.unwrap(type);
    }
}
