package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.core.metadata.unit.MeasurementUnit;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.metadata.unit.StandardUnit;
import org.jetlinks.core.metadata.unit.StandardValueUnit;

public class JetlinksStandardValueUnit extends StandardValueUnit implements Jsonable {

    public JetlinksStandardValueUnit() {
    }

    public JetlinksStandardValueUnit(StandardUnit unit) {
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

        setStandardUnit(MeasurementUnit.valueOf(type));

    }

    public static JetlinksStandardValueUnit of(StandardUnit conf) {
        JetlinksStandardValueUnit unit = new JetlinksStandardValueUnit();
        unit.setStandardUnit(conf);
        return unit;
    }

    public static JetlinksStandardValueUnit of(Object conf) {
        if (conf == null) {
            return null;
        }
        if (conf instanceof StandardUnit) {
            return of(((StandardUnit) conf));
        } else if (conf instanceof String) {
            MeasurementUnit unit = MeasurementUnit.valueOf(String.valueOf(conf));
            return new JetlinksStandardValueUnit(unit);
        } else if (conf instanceof JSONObject) {
            JetlinksStandardValueUnit unit = new JetlinksStandardValueUnit();
            unit.fromJson(((JSONObject) conf));
            return unit;
        }
        throw new UnsupportedOperationException("不支持的配置:" + conf);
    }
}
