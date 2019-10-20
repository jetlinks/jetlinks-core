package org.jetlinks.core.spi;

import org.jetlinks.core.Value;
import org.jetlinks.core.config.ConfigKey;

import java.util.List;
import java.util.Optional;

public interface ServiceContext {

    Optional<Value> getConfig(ConfigKey<String> key);

    Optional<Value> getConfig(String key);

    <T> Optional<T> getService(Class<T> service);

    <T> Optional<T> getService(String service);

    <T> List<T> getServices(Class<T> service);

    <T> List<T> getServices(String service);

}
