package org.jetlinks.core.command;

import lombok.AllArgsConstructor;
import org.jetlinks.core.metadata.FunctionMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * 对指定的命令支持的所有操作写入上下文,通常用于传递权限信息等操作.
 *
 * @author zhouhao
 * @see 1.2.2
 */
@AllArgsConstructor
public class ContextWriteCommandSupport implements CommandSupport {

    private final CommandSupport origin;

    private final Mono<Context> contextMono;

    @Override
    public boolean isWrapperFor(Class<?> type) {
        return origin.isWrapperFor(type);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return origin.unwrap(type);
    }

    @Nonnull
    @Override
    @SuppressWarnings("all")
    public <R> R execute(@Nonnull Command<R> command) {
        if (CommandUtils.commandResponseFlux(command)) {
            return (R) contextMono
                .flatMapMany(ctx -> origin
                    .executeToFlux(command)
                    .contextWrite(ctx));
        }
        if (CommandUtils.commandResponseMono(command)) {
            return (R) contextMono
                .flatMap(ctx -> origin
                    .executeToMono(command)
                    .contextWrite(ctx));
        }
        return origin.execute(command);
    }

    @Override
    public Flux<Object> executeToFlux(Command<?> command) {
        return contextMono
            .flatMapMany(ctx -> origin
                .executeToFlux(command)
                .contextWrite(ctx));
    }

    @Override
    public Flux<Object> executeToFlux(String commandId, Map<String, Object> parameters) {
        return contextMono
            .flatMapMany(ctx -> origin
                .executeToFlux(commandId, parameters)
                .contextWrite(ctx));
    }

    @Override
    public Flux<Object> executeToFlux(String commandId, Map<String, Object> parameters, Flux<Object> stream) {
        return contextMono
            .flatMapMany(ctx -> origin
                .executeToFlux(commandId, parameters, stream)
                .contextWrite(ctx));
    }

    @Override
    public Mono<Object> executeToMono(Command<?> command) {
        return contextMono
            .flatMap(ctx -> origin
                .executeToMono(command)
                .contextWrite(ctx));
    }

    @Override
    public Mono<Object> executeToMono(String commandId, Map<String, Object> parameters) {
        return contextMono
            .flatMap(ctx -> origin
                .executeToMono(commandId, parameters)
                .contextWrite(ctx));
    }

    @Override
    public <R, C extends Command<R>> C createCommand(String commandId) {
        return origin.createCommand(commandId);
    }

    @Override
    public <R, C extends Command<R>> Mono<C> createCommandAsync(String commandId) {
        return contextMono
            .flatMap(ctx -> origin
                .<R, C>createCommandAsync(commandId)
                .contextWrite(ctx));
    }

    @Override
    public Flux<FunctionMetadata> getCommandMetadata() {
        return contextMono
            .flatMapMany(ctx -> origin
                .getCommandMetadata()
                .contextWrite(ctx));
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(String commandId) {
        return contextMono
            .flatMap(ctx -> origin
                .getCommandMetadata(commandId)
                .contextWrite(ctx));
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(Command<?> command) {
        return contextMono
            .flatMap(ctx -> origin
                .getCommandMetadata(command)
                .contextWrite(ctx));
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(@Nonnull String commandId,
                                                     @Nullable Map<String, Object> parameters) {
        return contextMono
            .flatMap(ctx -> origin
                .getCommandMetadata(commandId,parameters)
                .contextWrite(ctx));
    }

    @Override
    public Mono<Boolean> commandIsSupported(Command<?> cmd) {
        return contextMono
            .flatMap(ctx -> origin
                .commandIsSupported(cmd)
                .contextWrite(ctx));
    }

    @Override
    public Mono<Boolean> commandIsSupported(Class<? extends Command<?>> cmd) {
        return contextMono
            .flatMap(ctx -> origin
                .commandIsSupported(cmd)
                .contextWrite(ctx));
    }

    @Override
    public Mono<Boolean> commandIsSupported(String commandId) {
        return contextMono
            .flatMap(ctx -> origin
                .commandIsSupported(commandId)
                .contextWrite(ctx));
    }
}
