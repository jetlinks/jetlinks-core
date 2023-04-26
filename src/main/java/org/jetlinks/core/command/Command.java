package org.jetlinks.core.command;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.Wrapper;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * 命令接口,用于定义一个命令,参数可以通过{@link #with(String, Object)}方法进行设置
 *
 * @param <Response> 命令对应的响应类型
 * @author zhouhao
 * @see ExecutableCommand
 * @since 1.2.1
 */
public interface Command<Response> extends Wrapper, Serializable {

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
     * 校验命令是否合法,如果不合法将抛出异常
     */
    default void validate() {

    }

}
