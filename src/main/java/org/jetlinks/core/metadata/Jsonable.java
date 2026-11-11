package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSONObject;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.exception.I18nSupportException;

/**
 * 元数据的 JSON 转换契约，默认使用 Bean 属性映射，具体模型可覆盖其编解码规则。
 * 目录解析可复用对象形状校验；转换与校验不执行外部 I/O。
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface Jsonable {

    /**
     * 校验目录条目为对象；根模型的可选空输入由各模型自行处理。
     *
     * @param value JSON 对象、Map 或可转换为对象的元数据，不能为 null
     * @return 转换后的对象，不修改输入
     * @throws I18nSupportException 条目为空或不是对象
     * @since 1.3.3
     */
    static JSONObject requireObject(Object value) {
        Object json = JSONObject.toJSON(value);
        if (!(json instanceof JSONObject)) {
            throw new I18nSupportException.NoStackTrace("error.invalid_metadata_definition");
        }
        return (JSONObject) json;
    }

    default JSONObject toJson() {
        return FastBeanCopier.copy(this, JSONObject::new);
    }

    default void fromJson(JSONObject json) {
        FastBeanCopier.copy(json, this);
    }
}
