package org.jetlinks.core.command.service;

import org.jetlinks.core.Module;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.command.blocking.BlockingCommandSupport;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 命令服务.
 *
 * @author zhouhao
 * @since 1.3.2
 */
public interface CommandService extends CommandSupport {

    /**
     * @return ID
     */
    String getId();

    /**
     * @return 支持的模块
     */
    Flux<Module> getModules();

    /**
     * 获取模块命令支持,用于执行模块命令.
     *
     * @param module 模块标识
     * @return CommandSupport
     */
    Mono<CommandSupport> getModule(String module);

    /**
     * 获取支持阻塞操作的模块命令支持
     *
     * @param module 模块标识
     * @return BlockingCommandSupport
     */
    default BlockingCommandSupport getModuleNow(String module) {
        return BlockingCommandSupport.of(getModule(module));
    }

}
