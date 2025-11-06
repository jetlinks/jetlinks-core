package org.jetlinks.core.command.bridge;

import org.jetlinks.core.Module;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.monitor.Monitor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 命令桥接提供商,用于创建命令交接器,实现功能解耦.
 * <p>
 * 比如将某个业务模块的设备,关联到iot的设备,并进行交互.也可以通过其他方式,比如接入第三方系统.
 *
 * @author zhouhao
 * @since 1.3.2
 */
public interface CommandBridgeProvider {

    /**
     * @return 唯一标识
     */
    String getId();

    /**
     * 获取名称,建议实现国际化支持.
     *
     * @return 名称
     * @see org.hswebframework.web.i18n.LocaleUtils#resolveMessage(String, Object...)
     */
    String getName();

    /**
     * 获取支持的模块
     *
     * @return 模块
     */
    Flux<Module> getModules();

    /**
     * 创建一个桥接器,用于执行命令.
     *
     * @param configuration BridgeConfiguration
     */
    Mono<CommandSupport> create(BridgeConfiguration configuration);


    interface BridgeConfiguration {

        /**
         * 目标模块
         *
         * @return 模块标识
         * @see CommandBridgeProvider#getModules()
         */
        String module();

        /**
         * @return 配置信息
         */
        Map<String, Object> configuration();

        /**
         * 监控接口,用于打印日志等.
         *
         * @return Monitor
         */
        Monitor monitor();

        /**
         * 获取平台所提供的模块,执行平台的相关命令.
         * <p>
         * 桥接器通常需要实现相同的命令来实现双向同步操作.
         *
         * @param module 服务支持标识
         * @return CommandSupport
         */
        CommandSupport getModule(String module);
    }
}
