package org.jetlinks.core.metadata.types;

import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertNotNull;

public class ObjectTypeTest extends JsonableTestBase<ObjectType> {

    @Override
    protected ObjectType newInstance() {
        return new ObjectType();
    }

    @Override
    protected void fillSampleData(ObjectType instance) {
        List<PropertyMetadata> list = Arrays.asList(
                SimplePropertyMetadata.of("id", "ID", new StringType()),
                SimplePropertyMetadata.of("info", "详情", new ObjectType())
        );
        instance.setProperties(list);
    }

    @Override
    protected void assertSampleData(ObjectType instance) {
        List<PropertyMetadata> properties = instance.getProperties();
        assertNotNull(properties);
        Assert.assertEquals(2, properties.size());
        PropertyMetadata id = properties.get(0);
        Assert.assertEquals("id", id.getId());
        Assert.assertEquals("ID", id.getName());
        Assert.assertEquals(StringType.ID, id.getValueType().getId());
        PropertyMetadata info = properties.get(1);
        Assert.assertEquals("info", info.getId());
        Assert.assertEquals("详情", info.getName());
        Assert.assertEquals(ObjectType.ID, info.getValueType().getId());


    }

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