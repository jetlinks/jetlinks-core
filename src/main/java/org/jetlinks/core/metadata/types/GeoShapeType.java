package org.jetlinks.core.metadata.types;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jetlinks.core.metadata.ValidateResult;

import java.util.Map;

public class GeoShapeType extends AbstractType<GeoShapeType> {
    public static final String ID = "geoShape";
    public static final GeoShapeType GLOBAL = new GeoShapeType();

    @Override
    public ValidateResult validate(Object value) {

        if (null == format(value)) {
            return ValidateResult.builder()
                    .success(false)
                    .errorMsg("不支持的GepShape格式:" + value)
                    .build();
        }
        return ValidateResult.success();
    }

    @Override
    public GeoShape format(Object value) {
        if (value instanceof GeoShape) {
            return ((GeoShape) value);
        }
        if (value instanceof GeoPoint) {
            return GeoShape.fromPoint(((GeoPoint) value));
        }
        if (value instanceof String && ((String) value).startsWith("{")) {
            return JSON.parseObject(String.valueOf(value), GeoShape.class);
        }
        if (value instanceof Map) {
            return new JSONObject(((Map) value)).toJavaObject(GeoShape.class);
        }
        return null;
    }

    @Override
    public String getId() {
        return "geoShape";
    }

    @Override
    public String getName() {
        return "地理地形";
    }

    @Override
    public String getDescription() {
        return null;
    }
}
