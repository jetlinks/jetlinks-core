package org.jetlinks.core.spi;

import org.jetlinks.core.Value;
import org.jetlinks.core.config.ConfigKey;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class EmptyServiceContext implements ServiceContext{
    public static final EmptyServiceContext INSTANCE = new EmptyServiceContext();

    @Override
    public Optional<Value> getConfig(ConfigKey<String> key) {
        return Optional.empty();
    }

    @Override
    public Optional<Value> getConfig(String key) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getService(Class<T> service) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getService(String service) {
        return Optional.empty();
    }

    @Override
    public <T> List<T> getServices(Class<T> service) {
        return Collections.emptyList();
    }

    @Override
    public <T> List<T> getServices(String service) {
        return Collections.emptyList();
    }
}
