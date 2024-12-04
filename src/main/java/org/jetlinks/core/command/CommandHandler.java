package org.jetlinks.core.command;

import org.jetlinks.core.metadata.FunctionMetadata;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 命令处理器,用于实现具体的命令处理逻辑
 *
 * @param <C>        命令类型
 * @param <Response> 命令执行结果
 * @author zhouhao
 * @since 1.2.0
 */
public interface CommandHandler<C extends Command<Response>, Response> {

    /**
     * 处理命令
     *
     * @param command 命令
     * @param support 命令支持
     * @return 命令执行结果
     */
    Response handle(@Nonnull C command, @Nonnull CommandSupport support);

    /**
     * 创建命令
     * @return 命令实例
     */
    @Nonnull
    C createCommand();

    /**
     * 获取命令元数据
     *
     * @return 元数据
     */
    FunctionMetadata getMetadata();

    /**
     * 通过lambda创建命令处理器
     * @param metadata 元数据
     * @param executor 执行器
     * @param commandBuilder 命令构建器
     * @return 命令处理器
     * @param <R> 命令执行结果类型
     * @param <C> 命令类型
     */
    static <R, C extends Command<R>> CommandHandler<C, R> of(FunctionMetadata metadata,
                                                             BiFunction<C, CommandSupport, R> executor,
                                                             Supplier<C> commandBuilder) {
        return of(() -> metadata, executor, commandBuilder);
    }

    /**
     * 通过lambda创建命令处理器
     * @param metadata 元数据
     * @param executor 执行器
     * @param commandBuilder 命令构建器
     * @return 命令处理器
     * @param <R> 命令执行结果类型
     * @param <C> 命令类型
     */
    static <R, C extends Command<R>> CommandHandler<C, R> of(Supplier<FunctionMetadata> metadata,
                                                             BiFunction<C, CommandSupport, R> executor,
                                                             Supplier<C> commandBuilder) {
        return new LambdaCommandHandler<>(metadata, executor, commandBuilder);
    }

}
