package org.jetlinks.core.metadata.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.FormatSupport;
import org.jetlinks.core.metadata.ValidateResult;
import org.jetlinks.core.metadata.unit.ValueUnits;

import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class GeoType extends AbstractType<GeoType> implements DataType, FormatSupport, Converter<GeoPoint> {
    public static final String ID = "geoPoint";

    public static final GeoType GLOBAL = new GeoType();

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
        return LocaleUtils.resolveMessage("message.metadata.type.geoPoint", LocaleUtils.current(), "地理位置");
    }

    public Map<String, Object> convertToMap(Object value) {
        Map<String, Object> mapGeoPoint = new HashMap<>();

        GeoPoint point = convert(value);
        if (point != null) {
            mapGeoPoint.put("lat", point.getLat());
            mapGeoPoint.put("lon", point.getLon());
        }
        return mapGeoPoint;
    }

    public GeoPoint convert(Object value) {
        return GeoPoint.of(value);
    }

    @Override
    public ValidateResult validate(Object value) {

        GeoPoint geoPoint = convert(value);

        return geoPoint == null
                ? ValidateResult.fail("不支持的Geo格式:" + value)
                : ValidateResult.success(geoPoint);
    }

    @Override
    public String format(Object value) {
        GeoPoint geoPoint = convert(value);

        return String.valueOf(geoPoint);
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        json.put("latProperty", this.getLatProperty());
        json.put("lonProperty", this.getLonProperty());
        return json;
    }

    @Override
    public void fromJson(JSONObject json) {
        super.fromJson(json);
        ofNullable(json.getString("latProperty"))
                .ifPresent(this::latProperty);
        ofNullable(json.getString("lonProperty"))
                .ifPresent(this::lonProperty);
    }


}
