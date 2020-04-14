package org.jetlinks.core.metadata.types;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.*;

/**
 * 地理地形
 *
 * @author zhouhao
 * @since 1.1
 */
@Getter
@Setter
public class GeoShape implements Serializable {

    //类型
    private Type type;

    //坐标,类型不同,坐标维度不同
    private List<Object> coordinates;

    public enum Type {
        Point,//点
        MultiPoint, //多个点
        LineString,//线
        MultiLineString,//多条线
        Polygon,//多边形
        MultiPolygon,//多个多边形
        GeometryCollection//数据集合,包含点线
        ;

        public static Type of(Object val) {
            for (Type value : values()) {
                if (value.name().equalsIgnoreCase(String.valueOf(val))) {
                    return value;
                }
            }
            throw new IllegalArgumentException("unsupported GeoShape type:" + val);
        }

        public List<Object> parseCoordinates(Object coordinates) {
            if (coordinates instanceof Collection) {
                return new ArrayList<>(((Collection<?>) coordinates));
            }
            if (coordinates instanceof String) {
                if (((String) coordinates).startsWith("[")) {
                    return JSON.parseArray(String.valueOf(coordinates));
                }
                return new ArrayList<>(Arrays.asList(((String) coordinates).split(",")));
            }
            throw new IllegalArgumentException("unsupported coordinates type :" + coordinates);
        }
    }

    public static GeoShape of(Object value) {
        if (value instanceof GeoShape) {
            return ((GeoShape) value);
        }
        if (value instanceof GeoPoint) {
            return GeoShape.fromPoint(((GeoPoint) value));
        }
        if (value instanceof String && ((String) value).startsWith("{")) {
            value = JSON.parseObject(String.valueOf(value));
        }
        if (value instanceof Map) {
            return GeoShape.of(((Map) value));
        }
        throw new IllegalArgumentException("unsupported GeoShape:" + value);
    }

    public static GeoShape of(Map<String, Object> map) {
        GeoShape shape = new GeoShape();
        shape.type = Type.of(map.get("type"));
        shape.coordinates = shape.type.parseCoordinates(map.get("coordinates"));
        return shape;
    }

    public static GeoShape fromPoint(GeoPoint point) {
        GeoShape shape = new GeoShape();
        shape.type = Type.Point;
        shape.coordinates = new ArrayList<>(Arrays.asList(point.getLon(), point.getLat()));
        return shape;
    }
}
