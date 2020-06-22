package org.jetlinks.core.metadata.types;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GeoPoint implements Serializable {
    private static final long serialVersionUID = -6849794470754667710L;

    //经度
    private double lon;

    //纬度
    private double lat;


    public static GeoPoint of(Object val) {
        Object tmp = val;
        if (val instanceof GeoPoint) {
            return ((GeoPoint) val);
        }

        if (val instanceof String) {
            String strVal = String.valueOf(val);
            if (strVal.startsWith("{")) {
                // {"lon":"lon","lat":lat}
                val = JSON.parseObject(strVal);
            } else if (strVal.startsWith("[")) {
                // [lon,lat]
                val = JSON.parseArray(strVal);
            } else {
                // lon,lat
                val = strVal.split("[,]");
            }
        }
        //{"lat":lat,"lon":lon} or {"x":lon,"y":lat}
        if (val instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> mapVal = ((Map<Object, Object>) val);
            Object lon = mapVal.getOrDefault("lon", mapVal.get("x"));
            Object lat = mapVal.getOrDefault("lat", mapVal.get("y"));
            val = new Object[]{lon,lat};
        }
        //  [lon,lat]
        if (val instanceof Collection) {
            val = ((Collection<?>) val).toArray();
        }
        //  [lon,lat]
        if (val instanceof Object[]) {
            Object[] arr = ((Object[]) val);
            if (arr.length >= 2) {
                return new GeoPoint(new BigDecimal(String.valueOf(arr[0])).doubleValue(), new BigDecimal(String.valueOf(arr[1])).doubleValue());
            }
        }
        throw new IllegalArgumentException("unsupported geo format:" + tmp);
    }

    @Override
    public int hashCode() {

        int result = 1;

        long temp = Double.doubleToLongBits(lat);
        result = 31 * result + (int) (temp ^ temp >>> 32);

        temp = Double.doubleToLongBits(lon);
        result = 31 * result + (int) (temp ^ temp >>> 32);

        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof GeoPoint)) {
            return false;
        }

        GeoPoint other = (GeoPoint) obj;

        return Double.doubleToLongBits(lon) == Double.doubleToLongBits(other.lon) &&
                Double.doubleToLongBits(lat) == Double.doubleToLongBits(other.lat);
    }

    @Override
    public String toString() {
        return lon + "," + lat;
    }
}
