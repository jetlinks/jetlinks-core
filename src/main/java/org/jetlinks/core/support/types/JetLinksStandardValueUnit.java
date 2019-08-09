package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.core.metadata.unit.UnifyUnit;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.metadata.unit.StandardUnit;
import org.jetlinks.core.metadata.unit.StandardValueUnit;

public class JetLinksStandardValueUnit extends StandardValueUnit implements Jsonable {

    public JetLinksStandardValueUnit() {
    }

    public JetLinksStandardValueUnit(StandardUnit unit) {
        super(unit);
    }


    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        StandardUnit unit = getStandardUnit();

        json.put("id", unit.getId());
        json.put("type", unit.getType());

        return json;
    }

    @Override
    public void fromJson(JSONObject json) {
        String type = json.getString("id");

        setStandardUnit(UnifyUnit.valueOf(type));

    }

    public static JetLinksStandardValueUnit of(StandardUnit conf) {
        JetLinksStandardValueUnit unit = new JetLinksStandardValueUnit();
        unit.setStandardUnit(conf);
        return unit;
    }

    public static JetLinksStandardValueUnit of(Object conf) {
        if (conf == null) {
            return null;
        }
        if (conf instanceof StandardUnit) {
            return of(((StandardUnit) conf));
        } else if (conf instanceof String) {
            UnifyUnit unit = UnifyUnit.valueOf(String.valueOf(conf));
            return new JetLinksStandardValueUnit(unit);
        } else if (conf instanceof JSONObject) {
            JetLinksStandardValueUnit unit = new JetLinksStandardValueUnit();
            unit.fromJson(((JSONObject) conf));
            return unit;
        }
        throw new UnsupportedOperationException("不支持的配置:" + conf);
    }
}
