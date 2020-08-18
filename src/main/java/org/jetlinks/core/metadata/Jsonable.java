package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSONObject;
import org.hswebframework.web.bean.FastBeanCopier;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface Jsonable {

    default JSONObject toJson() {
        return FastBeanCopier.copy(this, JSONObject::new);
    }

    default void fromJson(JSONObject json) {
        FastBeanCopier.copy(json, this);
    }
}
