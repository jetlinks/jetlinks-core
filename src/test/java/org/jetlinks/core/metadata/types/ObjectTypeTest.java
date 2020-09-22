package org.jetlinks.core.metadata.types;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class ObjectTypeTest {


    @Test
    public void testJSON() {
        ObjectType type = new ObjectType()
                .addProperty("timestamp", DateTimeType.GLOBAL)
                .addProperty("val", DoubleType.GLOBAL);


        Map<String, Object> val = type.convert("{\"timestamp\":\"1600262329000\",\"val\":3.24}");

        Assert.assertEquals(val.get("timestamp"), new Date(1600262329000L));
        Assert.assertEquals(val.get("val"), 3.24D);

    }


    @Test
    public void testMap() {
        ObjectType type = new ObjectType()
                .addProperty("timestamp", DateTimeType.GLOBAL);


        Map<String, Object> val = type.convert(Collections.singletonMap("timestamp", "1600262329000"));

        Assert.assertEquals(val.get("timestamp"), new Date(1600262329000L));

    }

}