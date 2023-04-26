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
     *
     * @param command 命令
     * @param <R>     结果类型
     * @return 执行结果
     * @see AbstractCommand
     */
    @Nonnull
    <R> R execute(@Nonnull Command<R> command);

    /**
     * 基于命令ID来创建命令,可通过{@link CommandSupport#getCommandMetadata()}来获取命令的定义信息.
     *
     * @param commandId 命令ID
     * @param <R>       命令结果类型
     * @param <C>       命令类型
     * @return 命令
     */
    default <R, C extends Command<R>> C createCommand(String commandId) {
        throw new CommandException(this, null, "error.unsupported_command", null, commandId);
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
        return getCommandMetadata()
                .filter(cmd -> Objects.equals(cmd.getId(), commandId))
                .singleOrEmpty();
    }
}
