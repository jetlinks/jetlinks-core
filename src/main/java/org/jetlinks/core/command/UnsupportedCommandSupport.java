package org.jetlinks.core.command;

import org.jetlinks.core.metadata.FunctionMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * 不提供命令能力的共享句柄。同步执行立即拒绝，响应式执行在订阅时拒绝；
 * 命令目录为空，取得句柄不执行 I/O。
 *
 * @since 1.3.3
 * @see CommandSupport
 */
public final class UnsupportedCommandSupport implements CommandSupport {
    public static final CommandSupport INSTANCE = new UnsupportedCommandSupport();

    private UnsupportedCommandSupport() {
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
