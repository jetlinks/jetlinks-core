package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private List<?> coordinates;

    public enum Type {
        Point,//点
        MultiPoint, //多个点
        LineString,//线
        MultiLineString,//多条线
        Polygon,//多边形
        MultiPolygon,//多个多边形
        GeometryCollection//数据集合,包含点线
    }

    public static GeoShape fromPoint(GeoPoint point) {
        GeoShape shape = new GeoShape();
        shape.type = Type.Point;
        shape.coordinates = new ArrayList<>(Arrays.asList(point.getLon(), point.getLat()));
        return shape;
    }
}
