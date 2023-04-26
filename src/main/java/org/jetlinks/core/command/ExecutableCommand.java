package org.jetlinks.core.command;

/**
 * 可执行的命令
 *
 * @param <Response> 命令响应类型
 * @author zhouhao
 * @since 1.2.1
 */
public interface ExecutableCommand<Response> extends Command<Response> {

    /**
     * 基于命令支持执行命令
     *
     * @param support 命令支持
     * @return 结果
     */
    Response execute(CommandSupport support);

}
