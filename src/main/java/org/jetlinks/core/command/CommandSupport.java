package org.jetlinks.core.command;

import org.jetlinks.core.Wrapper;
import org.jetlinks.core.metadata.FunctionMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

/**
 * 命令模式支持的统一定义接口,用于表示支持命令模式.
 *
 * @author zhouhao
 * @since 1.2.1
 */
public interface CommandSupport extends Wrapper {

    /**
     * 执行命令
     * <p>
     * 建议使用{@link CommandSupport#createCommand(String)}来动态创建命令,
     * 然后通过{@link Command#with(Map)}来设置命令参数.
     * <p>
     * 在不明确Command的具体类型时,请使用 {@link CommandSupport#executeToFlux(Command)} }
     * 或者{@link CommandSupport#executeToMono(Command)}
     *
     * @param command 命令
     * @param <R>     结果类型
     * @return 执行结果
     * @see AbstractCommand
     * @see CommandSupport#executeToFlux(Command)
     * @see CommandSupport#executeToFlux(String, Map)
     * @see CommandSupport#executeToMono(String, Map)
     * @see CommandSupport#executeToMono(Command)
     */
    @Nonnull
    <R> R execute(@Nonnull Command<R> command);

    /**
     * 执行命令,并将结果转为Flux
     *
     * @param command 命令
     * @return Flux
     */
    default Flux<Object> executeToFlux(Command<?> command) {
        return CommandUtils.convertResponseToFlux(execute(command), command);
    }

    /**
     * 执行命令,并将结果转为Flux
     *
     * @param commandId  命令ID
     * @param parameters 参数
     * @return Flux
     */
    default Flux<Object> executeToFlux(String commandId, Map<String, Object> parameters) {
        return createCommandAsync(commandId)
            .flatMapMany(cmd -> this.executeToFlux(cmd.with(parameters)));
    }

    /**
     * 执行流式命令，通过响应式传递参数，通常用于传递大量数据的场景。
     *
     * @param commandId  命令ID
     * @param parameters 参数
     * @return Flux
     * @see StreamCommand
     */
    default Flux<Object> executeToFlux(String commandId,
                                       Map<String, Object> parameters,
                                       Flux<Object> stream) {
        return this
            .createCommandAsync(commandId)
            .flatMapMany(cmd -> {
                if (cmd.isWrapperFor(StreamCommand.class)) {
                    @SuppressWarnings("all")
                    StreamCommand<Object, Object> command = cmd.unwrap(StreamCommand.class);
                    command.withStream(stream.mapNotNull(command::convertStreamValue));
                } else {
                    return Flux.error(new CommandException.NoStackTrace(
                        this, cmd, "error.unsupported_command", null, cmd.getCommandId()));
                }
                return this.executeToFlux(cmd.with(parameters));
            });
    }

    /**
     * 执行命令,并将结果转为Mono
     *
     * @param command 命令
     * @return Mono
     */
    default Mono<Object> executeToMono(Command<?> command) {
        return CommandUtils.convertResponseToMono(execute(command), command);
    }

    /**
     * 执行命令,并将结果转为Mono
     *
     * @param commandId  命令ID
     * @param parameters 参数
     * @return Mono
     */
    default Mono<Object> executeToMono(String commandId, Map<String, Object> parameters) {
        return createCommandAsync(commandId)
            .flatMap(cmd -> this.executeToMono(cmd.with(parameters)));
    }

    /**
     * 基于命令ID来创建命令,可通过{@link CommandSupport#getCommandMetadata()}来获取命令的定义信息.
     *
     * @param commandId 命令ID
     * @param <R>       命令结果类型
     * @param <C>       命令类型
     * @return 命令
     * @see CommandSupport#createCommandAsync(String)
     */
    default <R, C extends Command<R>> C createCommand(String commandId) {
        throw new CommandException.NoStackTrace(this, null, "error.unsupported_command", null, commandId);
    }

    /**
     * 基于命令ID一步创建命令
     *
     * @param commandId 命令ID
     * @param <R>       命令结果类型
     * @param <C>       命令类型
     * @return Mono
     */
    default <R, C extends Command<R>> Mono<C> createCommandAsync(String commandId) {
        return Mono.fromSupplier(() -> this.<R, C>createCommand(commandId));
    }

    /**
     * 获取所有支持的命令元数据信息
     *
     * @return 命令元数据信息
     */
    default Flux<FunctionMetadata> getCommandMetadata() {
        return Flux.empty();
    }

    /**
     * 获取指定ID的命令元数据信息
     *
     * @param commandId 命令ID
     * @return 命令元数据信息
     */
    default Mono<FunctionMetadata> getCommandMetadata(String commandId) {
        return this
            .getCommandMetadata()
            .filter(cmd -> Objects.equals(cmd.getId(), commandId))
            .singleOrEmpty();
    }

    /**
     * 判断是否支持此命令
     *
     * @param cmd 命令
     * @return 是否支持
     */
    default Mono<Boolean> commandIsSupported(Command<?> cmd) {
        return commandIsSupported(cmd.getCommandId());
    }

    /**
     * 判断是否支持此命令
     *
     * @param cmd 命令类型
     * @return 是否支持
     */
    default Mono<Boolean> commandIsSupported(Class<? extends Command<?>> cmd) {
        return commandIsSupported(CommandUtils.getCommandIdByType(cmd));
    }

    /**
     * 判断是否支持此命令
     *
     * @param commandId 命令ID
     * @return 是否支持
     */
    default Mono<Boolean> commandIsSupported(String commandId) {
        return this
            .getCommandMetadata(commandId)
            .hasElement();
    }
}
