package org.jetlinks.core.metadata.types;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class GeoTypeTest extends JsonableTestBase<GeoType> {

    @Override
    protected GeoType newInstance() {
        return new GeoType();
    }

    @Override
    protected void fillSampleData(GeoType instance) {
        instance.latProperty("latitude");
        instance.lonProperty("longitude");
    }

    @Override
    protected void assertSampleData(GeoType instance) {
        assertEquals("latitude", instance.getLatProperty());
        assertEquals("longitude", instance.getLonProperty());
    }

    @Test
    public void testConvert() {
        GeoType type = new GeoType();

        GeoPoint point = type.convert("1234.112,1211.23");

        assertEquals(point.getLon(), 1234.112, 0);
        assertEquals(point.getLat(), 1211.23, 0);
    }

    @Test
    public void testConvertJsoArr() {
        GeoType type = new GeoType();

        GeoPoint point = type.convert("[1234.112,1211.23]");

        assertEquals(point.getLon(), 1234.112, 0);
        assertEquals(point.getLat(), 1211.23, 0);
    }

    @Test
    public void testConvertArr() {
        GeoType type = new GeoType();

        GeoPoint point = type.convert(new Object[]{1234.112, "1211.23"});

        assertEquals(point.getLon(), 1234.112, 0);
        assertEquals(point.getLat(), 1211.23, 0);
    }

    @Test
    public void testConvertMap() {
        GeoType type = new GeoType();

        Map<String, Object> latlon = new HashMap<>();
        latlon.put("lat", "1234.112");
        latlon.put("lon", "1211.211321");

        GeoPoint point = type.convert(latlon);

        assertEquals(point.getLat(), 1234.112, 0);
        assertEquals(point.getLon(), 1211.211321, 0);
    }


}