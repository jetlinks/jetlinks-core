package org.jetlinks.core.plugin;

import org.jetlinks.core.Wrapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 插件实例接口
 *
 * @author zhouhao
 * @since 1.1.9
 */
public interface Plugin extends Wrapper {

    /**
     * @return 插件实例ID
     */
    String getId();

    /**
     * 插件类型,不同的插件类型支持的命令不同,如网络服务插件等.
     *
     * @return 插件类型
     */
    PluginType getType();

    /**
     * @return 插件状态
     */
    PluginState getState();

    /**
     * @return 启动插件
     */
    Mono<Void> start();

    /**
     * @return 暂停插件
     */
    Mono<Void> pause();

    /**
     * @return 停止插件
     */
    Mono<Void> shutdown();

    /**
     * 执行命令,通过命令来实现同步的操作。
     * 不同的插件类型支持的命令不同
     *
     * @return 执行结果
     */
    <R> R execute(PluginCommand<R> command);

    /**
     * 根据命令ID来创建一个命令实例,用于动态创建命令的时候,
     * 如果不支持此命令将返回{@link Mono#empty()}
     *
     * @param commandId 命令ID
     * @param <R>       命令返回类型
     * @return 命令
     */
    <R> Mono<PluginCommand<R>> createCommand(String commandId);

    /**
     * 获取支持的命令信息
     *
     * @return 命令信息
     */
    Flux<CommandDescription> getSupportCommands();

}
