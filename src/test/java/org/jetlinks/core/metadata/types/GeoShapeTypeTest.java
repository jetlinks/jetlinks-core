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

    }


}