package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;

/**
 * 物模型数据类型
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DataType extends Metadata, FormatSupport, Jsonable {

    /**
     * 验证是否合法
     *
     * @param value 值
     * @return ValidateResult
     */
    ValidateResult validate(Object value);

    /**
     * @return 类型标识
     */
    default String getType() {
        return getId();
    }

    /**
     * @return 拓展属性
     */
    @Override
    default Map<String, Object> getExpands() {
        return null;
    }

    default JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("type", getType());
        return json;
    }


}
