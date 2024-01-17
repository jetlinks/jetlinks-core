package org.jetlinks.core.command;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.Wrapper;
import org.jetlinks.core.utils.ConverterUtils;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

/**
 * 命令接口,用于定义一个命令,参数可以通过{@link #with(String, Object)}方法进行设置
 *
 * @param <Response> 命令对应的响应类型
 * @author zhouhao
 * @see ExecutableCommand
 * @see AbstractCommand
 * @see CommandSupport
 * @see CommandSupport#createCommand(String)
 * @since 1.2.1
 */
public interface Command<Response> extends Wrapper, Serializable {

    /**
     * 返回命令ID
     *
     * @return 命令ID
     */
    default String getCommandId() {
        return CommandUtils.getCommandIdByType(this.getClass());
    }

    /**
     * 设置命令的单个参数
     *
     * @param key   key
     * @param value value
     * @return this
     */
    default Command<Response> with(String key, Object value) {
        FastBeanCopier.copy(this, Collections.singletonMap(key, value));
        return this;
    }

    /**
     * 设置命令的多个参数
     *
     * @param parameters 参数
     * @return this
     */
    default Command<Response> with(Map<String, Object> parameters) {
        if (null != parameters) {
            FastBeanCopier.copy(this, parameters);
        }
        return this;
    }

    /**
     * 获取命令的参数,当参数不存在时返回null.
     *
     * @param key  参数Key
     * @param type 期望类型
     * @param <T>  参数值类型
     * @return 参数值
     */
    default <T> T getOrNull(String key, Type type) {
        return ConverterUtils.convert(FastBeanCopier.getProperty(this, key), type);
    }

    /**
     * 获取命令的参数,当参数不存在时返回null.
     *
     * @param key  参数Key
     * @param type 期望类型
     * @param <T>  参数值类型
     * @return 参数值
     */
    default <T> T getOrNull(String key, Class<T> type) {
        return getOrNull(key, (Type) type);
    }


    /**
     * 校验命令是否合法,如果不合法将抛出异常
     */
    default void validate() {

    }

}
