package org.jetlinks.core.spi;

import org.jetlinks.core.Value;
import org.jetlinks.core.config.ConfigKey;

import java.util.List;
import java.util.Optional;

/**
 * 服务上下文,用于从服务中获取其他服务(如获取spring容器中的bean),配置等操作.
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface ServiceContext {

    Optional<Value> getConfig(ConfigKey<String> key);

    Optional<Value> getConfig(String key);

    <T> Optional<T> getService(Class<T> service);

    default <T> Optional<T> getService(String service,Class<T> type){
        return getService(service);
    }

    <T> Optional<T> getService(String service);

    <T> List<T> getServices(Class<T> service);

    <T> List<T> getServices(String service);

}
