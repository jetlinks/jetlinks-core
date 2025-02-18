package org.jetlinks.core.collector;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;

import java.util.Map;
import java.util.function.Supplier;

@Getter
@Setter
public class BaseProperties {

    private String id;

    private Map<String, Object> configuration;

    public <T> T copyTo(T target) {
        if (configuration != null) {
            FastBeanCopier.copy(configuration, target);
        }
        return target;
    }

    public <T> T as(Supplier<T> supplier) {
        T instance = supplier.get();
        if (configuration != null) {
            FastBeanCopier.copy(configuration, instance);
        }
        return instance;
    }
}
