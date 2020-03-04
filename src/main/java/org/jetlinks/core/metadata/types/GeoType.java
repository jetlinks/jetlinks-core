package org.jetlinks.core.metadata.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.FormatSupport;
import org.jetlinks.core.metadata.ValidateResult;

import java.util.Map;

@Getter
@Setter
public class GeoType extends AbstractType<GeoType> implements DataType, FormatSupport, Converter<GeoPoint> {
    public static final String ID = "geoPoint";

    //经度字段
    private String latProperty = "lat";

    //纬度字段
    private String lonProperty = "lon";

    public GeoType latProperty(String property) {
        this.latProperty = property;
        return this;
    }

    public GeoType lonProperty(String property) {
        this.lonProperty = property;
        return this;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "地理位置";
    }

    public GeoPoint convert(Object value) {
        if (value instanceof GeoPoint) {
            return ((GeoPoint) value);
        }
        if (value instanceof Map) {
            @SuppressWarnings("all")
            JSONObject json = new JSONObject(((Map) value));
            return new GeoPoint(json.getDoubleValue(latProperty), json.getDoubleValue(lonProperty));
        }
        if (value instanceof String) {
            String[] str = ((String) value).split("[,]");
            if (str.length == 2) {
                return new GeoPoint(Double.parseDouble(str[0]), Double.parseDouble(str[1]));
            }
        }
        return null;
    }

    @Override
    public ValidateResult validate(Object value) {

        GeoPoint geoPoint = convert(value);

        return geoPoint == null
                ? ValidateResult.fail("不支持的Geo格式:" + value)
                : ValidateResult.success();
    }

    @Override
    public String format(Object value) {
        GeoPoint geoPoint = convert(value);

        return String.valueOf(geoPoint);
    }


}
