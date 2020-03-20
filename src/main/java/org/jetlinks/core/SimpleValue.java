package org.jetlinks.core;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;

@AllArgsConstructor(staticName = "of")
class SimpleValue implements Value {

    private Object nativeValue;

    @Override
    public Object get() {
        return nativeValue;
    }

    @Override
    public <T> T as(Class<T> type) {
        if (nativeValue == null) {
            return null;
        }
        if(type.isInstance(nativeValue)){
            return (T)nativeValue;
        }
        return FastBeanCopier.DEFAULT_CONVERT.convert(
                nativeValue, type, FastBeanCopier.EMPTY_CLASS_ARRAY
        );
    }
}
