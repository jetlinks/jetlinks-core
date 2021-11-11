package org.jetlinks.core.metadata.types;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class GeoShapeTypeTest {

    @Test
    public void testPoint() {
        GeoShapeType type = GeoShapeType.GLOBAL;
        Map<String, Object> map = new HashMap<>();
        map.put("type", "point");
        map.put("coordinates", Arrays.asList(56.123, 121.123));
        GeoShape shape = type.convert(map);

        assertEquals(shape.getType(), GeoShape.Type.Point);
        assertArrayEquals(shape.getCoordinates().toArray(), new Object[]{56.123, 121.123});

        shape = GeoShape.point(GeoPoint.of(56.123, 121.123));
        assertEquals(shape.getType(), GeoShape.Type.Point);
        assertArrayEquals(shape.getCoordinates().toArray(), new Object[]{56.123, 121.123});

    }

    @Test
    public void testMultiPoint() {
        GeoShapeType type = GeoShapeType.GLOBAL;
        Map<String, Object> map = new HashMap<>();
        map.put("type", "MultiPoint");
        map.put("coordinates", Arrays.asList(Arrays.asList(56.123, 121.123), Arrays.asList(56.123, 123.123)));
        GeoShape shape = type.convert(map);

        assertEquals(shape.getType(), GeoShape.Type.MultiPoint);
        assertEquals(shape.getCoordinates(), Arrays.asList(Arrays.asList(56.123, 121.123), Arrays.asList(56.123, 123.123)));
        shape = GeoShape.multiPoint(GeoPoint.of(56.123, 121.123), GeoPoint.of(56.123, 123.123));
        assertEquals(shape.getType(), GeoShape.Type.MultiPoint);
        assertEquals(shape.getCoordinates(), Arrays.asList(Arrays.asList(56.123D, 121.123D), Arrays.asList(56.123D, 123.123D)));
    }


    @Test
    public void testLineString() {
        GeoShape shape = GeoShape.lineString(GeoPoint.of(56.123, 121.123), GeoPoint.of(56.123, 123.123));
        assertEquals(shape.getType(), GeoShape.Type.LineString);
        assertEquals(shape.getCoordinates(), Arrays.asList(Arrays.asList(56.123D, 121.123D), Arrays.asList(56.123D, 123.123D)));
    }

    @Test
    public void testMultiLineString() {
        GeoShape shape = GeoShape.multiLineString(Arrays
                                                          .asList(
                                                                  GeoPoint.of(56.123, 121.123), GeoPoint.of(56.123, 123.123)
                                                          ),
                                                  Arrays
                                                          .asList(
                                                                  GeoPoint.of(52.123, 121.123), GeoPoint.of(52.123, 123.123)
                                                          )
        );
        assertEquals(shape.getType(), GeoShape.Type.MultiLineString);
        assertEquals(shape.getCoordinates(), Arrays.asList(
                Arrays.asList(Arrays.asList(56.123D, 121.123D), Arrays.asList(56.123D, 123.123D)),
                Arrays.asList(Arrays.asList(52.123D, 121.123D), Arrays.asList(52.123D, 123.123D))
        ));
    }

    @Test
    public void testPolygon() {
        GeoShape shape = GeoShape.polygon(Arrays
                                                  .asList(
                                                          GeoPoint.of(56.123, 121.123), GeoPoint.of(56.123, 123.123)
                                                  ),
                                          Arrays
                                                  .asList(
                                                          GeoPoint.of(52.123, 121.123), GeoPoint.of(52.123, 123.123)
                                                  )
        );
        assertEquals(shape.getType(), GeoShape.Type.Polygon);
        assertEquals(shape.getCoordinates(), Arrays.asList(
                Arrays.asList(Arrays.asList(56.123D, 121.123D), Arrays.asList(56.123D, 123.123D)),
                Arrays.asList(Arrays.asList(52.123D, 121.123D), Arrays.asList(52.123D, 123.123D))
        ));
    }

    @Test
    public void testMultiPolygon() {
        GeoShape shape = GeoShape.multiPolygon(
                Arrays.asList(
                        Arrays.asList(GeoPoint.of(56.123, 121.123), GeoPoint.of(56.123, 123.123)),
                        Arrays.asList(GeoPoint.of(52.123, 121.123), GeoPoint.of(52.123, 123.123))
                ),
                Arrays.asList(
                        Arrays.asList(GeoPoint.of(56.123, 121.123), GeoPoint.of(56.123, 123.123)),
                        Arrays.asList(GeoPoint.of(51.123, 121.123), GeoPoint.of(51.123, 123.123))
                )
        );
        assertEquals(shape.getType(), GeoShape.Type.MultiPolygon);
        System.out.println(shape.getCoordinates());
        assertEquals(shape.getCoordinates(), Arrays.asList(
                Arrays.asList(
                        Arrays.asList(Arrays.asList(56.123D, 121.123D), Arrays.asList(56.123D, 123.123D)),
                        Arrays.asList(Arrays.asList(52.123D, 121.123D), Arrays.asList(52.123D, 123.123D))
                ),
                Arrays.asList(
                        Arrays.asList(Arrays.asList(56.123D, 121.123D), Arrays.asList(56.123D, 123.123D)),
                        Arrays.asList(Arrays.asList(51.123D, 121.123D), Arrays.asList(51.123D, 123.123D))
                )
        ));
    }

    @Test
    public void testCollection() {
        GeoShape shape= GeoShape.collection(GeoShape.point(GeoPoint.of(12.11,13.22)),GeoShape.point(GeoPoint.of(22.22,33.33)));
        assertEquals(shape.getType(),GeoShape.Type.GeometryCollection);
        System.out.println(shape);
    }

}