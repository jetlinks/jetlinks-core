package org.jetlinks.core.metadata;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MergeOptionTest {


    @Test
    public void testMerge() {
        Map<String,Object> from =new HashMap<>();
        from.put("id","123");
        from.put("name","1234");

        Map<String,Object> to =new HashMap<>();
        to.put("id","321");
        to.put("name","4321");


        MergeOption.ExpandsMerge.doWith(DeviceMetadataType.property,
                                        from,
                                        to
        );
        assertEquals(to.get("id"),"123");
        assertEquals(to.get("name"),"1234");

    }

    @Test
    public void testIgnore() {

        MergeOption option = MergeOption.ExpandsMerge.ignore("id", "name");

        Map<String,Object> from =new HashMap<>();
        from.put("id","123");
        from.put("name","1234");

        Map<String,Object> to =new HashMap<>();
        to.put("id","321");
        to.put("name","4321");


        MergeOption.ExpandsMerge.doWith(DeviceMetadataType.property,
                                        from,
                                        to,
                                        option
                                        );

        assertEquals(to.get("id"),"321");
        assertEquals(to.get("name"),"4321");

    }


    @Test
    public void testRemove() {

        MergeOption option = MergeOption.ExpandsMerge.remove("id", "name");

        Map<String,Object> from =new HashMap<>();
        from.put("id","123");
        from.put("name","1234");

        Map<String,Object> to =new HashMap<>();
        to.put("id","321");
        to.put("name","4321");


        MergeOption.ExpandsMerge.doWith(DeviceMetadataType.property,
                                        from,
                                        to,
                                        option
        );

        assertTrue(to.isEmpty());

    }
}