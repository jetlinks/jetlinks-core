package org.jetlinks.core.command.service;

import org.jetlinks.core.monitor.Monitor;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 命令服务提供商,用于动态创建一个命令服务.
 *
 * @author zhouhao
 * @since 1.3.2
 */
public interface CommandServiceProvider {

    /**
     * @return 服务提供商标识
     */
    String getId();

    /**
     * 创建命令服务
     *
     * @param configuration CommandServiceConfiguration
     * @return 命令服务
     */
    Mono<CommandService> create(CommandServiceConfiguration configuration);

    interface CommandServiceConfiguration {

        String getId();

        Map<String, Object> getConfig();

        Monitor monitor();
    }

}
