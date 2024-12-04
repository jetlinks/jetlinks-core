package org.jetlinks.core.command;

/**
 * 基于泛型输入的命令,与{@link InputSpec}配合使用,实现泛型输入命令解析.
 *
 * @param <T>
 * @see InputSpec
 */
public interface GenericInputCommand<T> {

    /**
     * 将命令实现类中定义的InputSpec实现此接口
     *
     * @param <T> 类型
     */
    interface InputSpec<T> {

    }
}
