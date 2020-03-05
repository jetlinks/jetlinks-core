package org.jetlinks.core.metadata.types;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class GeoTypeTest {

    @Test
    public void testConvert() {
        GeoType type = new GeoType();

        GeoPoint point = type.convert("1234.112,1211.23");

        Assert.assertEquals(point.getLat(), 1234.112, 0);
        Assert.assertEquals(point.getLon(), 1211.23, 0);
    }

    @Test
    public void testConvertMap() {
        GeoType type = new GeoType();

        Map<String,Object> latlon=new HashMap<>();
        latlon.put("lat","1234.112");
        latlon.put("lon","1211.211321");

        GeoPoint point = type.convert(latlon);

        Assert.assertEquals(point.getLat(), 1234.112, 0);
        Assert.assertEquals(point.getLon(), 1211.211321, 0);
    }


}