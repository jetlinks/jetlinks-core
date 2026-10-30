package org.jetlinks.core.things.relation;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.core.metadata.Jsonable;
import org.hswebframework.web.exception.I18nSupportException;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public interface Relation extends Jsonable {
    String getId();

    String getName();

    /**
     * 反转关系名称
     */
    default String getReverseName() {
        return null;
    }

    /**
     * 是否为反转关系
     */
    default boolean isReverse() {
        return false;
    }

    default Map<String, Object> getExpands() {
        return Collections.emptyMap();
    }

    /**
     * 编码规范定义/反向投影，不包含对象实例及执行权限。
     * @return 纯 JSON 模型
     * @since 1.3.3
     */
    @Override
    default JSONObject toJson() {
        JSONObject json = new JSONObject(true);
        json.put("id", getId());
        json.put("name", getName());
        json.put("reverseName", getReverseName());
        json.put("reverse", isReverse());
        json.put("expands", getExpands());
        return json;
    }

    /**
     * 读取关系定义快照；扩展词保留，模型不依赖来源业务实现类。
     * @param json 原模型关系定义
     * @return 不可变关系视图
     * @since 1.3.3
     */
    static Relation parse(JSONObject json) {
        return new JsonRelation(json);
    }
}

final class JsonRelation implements Relation, Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String name;
    private final String reverseName;
    private final boolean reverse;
    private final Map<String, Object> expands;

    JsonRelation(JSONObject json) {
        id = json.getString("id");
        if (id == null || id.trim().isEmpty()) {
            throw new I18nSupportException.NoStackTrace("error.invalid_relation_definition", "id");
        }
        name = json.getString("name");
        reverseName = json.getString("reverseName");
        reverse = json.getBooleanValue("reverse");
        JSONObject extra = json.getJSONObject("expands");
        expands = extra == null ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(extra));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getReverseName() {
        return reverseName;
    }

    @Override
    public boolean isReverse() {
        return reverse;
    }

    @Override
    public Map<String, Object> getExpands() {
        return expands;
    }
}
