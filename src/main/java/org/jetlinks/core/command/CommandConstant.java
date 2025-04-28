package org.jetlinks.core.command;

import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.metadata.Metadata;

/**
 * 命令相关通用常量定义类
 *
 * @author zhouhao
 * @since 1.2.3
 */
public interface CommandConstant {

    /**
     * 标记命令为无界流
     *
     * @see org.jetlinks.core.metadata.FunctionMetadata#expand(String, Object)
     * @see org.jetlinks.core.annotation.command.Unbounded
     */
    String EXPANDS_UNBOUNDED = "unbounded";

    /**
     * 标记命令为无界流
     *
     * @see org.jetlinks.core.metadata.FunctionMetadata#expand(ConfigKey, Object)
     * @see org.jetlinks.core.annotation.command.Unbounded
     */
    ConfigKey<Boolean> UNBOUNDED = ConfigKey.of(EXPANDS_UNBOUNDED, "无界流", Boolean.class);

    /**
     * 标记命令响应为一个Flux流
     *
     * @see org.jetlinks.core.metadata.FunctionMetadata#expand(ConfigKey, Object)
     * @see reactor.core.publisher.Flux
     * @see CommandSupport#executeToFlux(Command)
     */
    ConfigKey<Boolean> responseFlux = ConfigKey.of("responseFlux", "Flux响应", Boolean.class);

    /**
     * 判断命令是否返回无界流
     *
     * @param metadata 命令元数据
     * @return true:无界流, false:有界流
     */
    static boolean isUnbounded(Metadata metadata) {
        return metadata.getExpand(UNBOUNDED).orElse(false);
    }

    /**
     * 判断命令是否返回Flux流
     *
     * @param metadata 命令元数据
     * @return true:Flux流, false:单值
     */
    static boolean isResponseFlux(Metadata metadata) {
        return metadata.getExpand(responseFlux).orElse(false);
    }
}
