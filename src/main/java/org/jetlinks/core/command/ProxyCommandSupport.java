package org.jetlinks.core.command;

import org.jetlinks.core.metadata.FunctionMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * 代理命令支持
 *
 * @author zhouhao
 * @since 1.2.2
 */
public interface ProxyCommandSupport extends CommandSupport {

    CommandSupport getProxyTarget();

    @Nonnull
    @Override
    default <R> R execute(@Nonnull Command<R> command) {
        return getProxyTarget().execute(command);
    }

    @Override
    default Flux<Object> executeToFlux(Command<?> command) {
        return getProxyTarget().executeToFlux(command);
    }

    @Override
    default Flux<Object> executeToFlux(String commandId, Map<String, Object> parameters) {
        return getProxyTarget().executeToFlux(commandId, parameters);
    }

    @Override
    default Flux<Object> executeToFlux(String commandId, Map<String, Object> parameters, Flux<Object> stream) {
        return getProxyTarget().executeToFlux(commandId, parameters, stream);
    }

    @Override
    default Mono<Boolean> commandIsSupported(Command<?> cmd) {
        return getProxyTarget().commandIsSupported(cmd);
    }

    @Override
    default Mono<Boolean> commandIsSupported(String commandId) {
        return getProxyTarget().commandIsSupported(commandId);
    }

    @Override
    default <R, C extends Command<R>> C createCommand(String commandId) {
        return getProxyTarget().createCommand(commandId);
    }

    @Override
    default <R, C extends Command<R>> Mono<C> createCommandAsync(String commandId) {
        return getProxyTarget().createCommandAsync(commandId);
    }

    @Override
    default Mono<Boolean> commandIsSupported(Class<? extends Command<?>> cmd) {
        return getProxyTarget().commandIsSupported(cmd);
    }

    @Override
    default Flux<FunctionMetadata> getCommandMetadata() {
        return getProxyTarget().getCommandMetadata();
    }

    @Override
    default Mono<FunctionMetadata> getCommandMetadata(String commandId) {
        return getProxyTarget().getCommandMetadata(commandId);
    }

    @Override
    default Mono<FunctionMetadata> getCommandMetadata(Command<?> command) {
        return getProxyTarget().getCommandMetadata(command);
    }

    @Override
    default Mono<FunctionMetadata> getCommandMetadata(@Nonnull String commandId,
                                                      @Nullable Map<String, Object> parameters) {
        return getProxyTarget().getCommandMetadata(commandId,parameters);
    }

    @Override
    default Mono<Object> executeToMono(Command<?> command) {
        return getProxyTarget().executeToMono(command);
    }

    @Override
    default Mono<Object> executeToMono(String commandId, Map<String, Object> parameters) {
        return getProxyTarget().executeToMono(commandId, parameters);
    }

    @Override
    default boolean isWrapperFor(Class<?> type) {
        return CommandSupport.super.isWrapperFor(type) || getProxyTarget().isWrapperFor(type);
    }

    @Override
    default <T> T unwrap(Class<T> type) {
        return CommandSupport.super.isWrapperFor(type)
            ? CommandSupport.super.unwrap(type)
            : getProxyTarget().unwrap(type);
    }
}
