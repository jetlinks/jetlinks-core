package org.jetlinks.core.command.service;

import org.jetlinks.core.Module;
import org.jetlinks.core.command.CommandSupport;
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
     * 获取服务描述
     *
     * @return 服务描述
     */
    Mono<ServiceDescription> getDescription();

    /**
     * 获取模块命令支持,用于执行模块命令.
     *
     * @param module 模块标识
     * @return CommandSupport
     */
    Mono<CommandSupport> getModule(String module);

    /**
     * 获取模块命令支持,用于执行模块命令.
     *
     * @param module 模块定义
     * @return CommandSupport
     */
    default Mono<CommandSupport> getModule(Module module) {
        return getModule(module.getId());
    }


}
