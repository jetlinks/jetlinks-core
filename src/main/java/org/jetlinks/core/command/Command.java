package org.jetlinks.core.command;

import lombok.SneakyThrows;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.Wrapper;
import org.jetlinks.core.utils.ConverterUtils;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Flux;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    /**
     * 创建一个新的响应结果数据,用于在一些动态调用的场景下,进行结果类型转换.
     *
     * @return 响应结果数据
     */
    @SneakyThrows
    default Object createResponseData() {
        return CommandUtils
            .getCommandResponseDataType(this)
            .toClass()
            .newInstance();
    }

    /**
     * 创建一个新的响应结果数据,并将指定的数据复制到新的结果数据中.
     *
     * @return 响应结果数据
     */
    default Object createResponseData(Object value) {
        return CommandUtils.convertData(
            CommandUtils.getCommandResponseDataType(this),
            value);
    }

    /**
     * 转换当前对象为map
     *
     * @return void
     * @see CommandSupport#executeToFlux(String, Map)
     * @see CommandSupport#executeToMono(String, Map)
     * @see Command#with(Map)
     * @since 1.2.2
     */
    default Map<String, Object> asMap() {
        return FastBeanCopier.copy(this, new HashMap<>());
    }
}
