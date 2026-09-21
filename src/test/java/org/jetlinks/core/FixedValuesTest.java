package org.jetlinks.core;

import org.junit.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import static org.junit.Assert.*;

public class FixedValuesTest {

    @Test
    public void testTwoKeys() {
        Values values = Values.of("id", "device-1", "missing", null);
        assertEquals(1, values.size());
        assertEquals("device-1", values.getString("id", (String) null));
        assertNull(values.getString("missing", (String) null));
        assertFalse(values.getValue("missing").isPresent());
        assertEquals(Collections.singletonMap("id", "device-1"), values.getAllValues());
        assertEquals(Collections.singletonList("missing"), new ArrayList<>(values.getNonExistentKeys(Arrays.asList("id", "missing"))));
        assertEquals("override", values.merge(Values.of(Collections.singletonMap("id", "override")))
                                       .getString("id", (String) null));
    }

    @Test
    public void testThreeKeys() {
        Values values = Values.of("id", "device-1", "name", "demo", "missing", null);
        assertEquals(2, values.size());
        Map<String, Object> expected = new HashMap<>();
        expected.put("id", "device-1");
        expected.put("name", "demo");
        assertEquals(expected, values.getAllValues());
        assertEquals("demo", values.getValue("name").get().asString());
        assertEquals(Collections.singletonList("missing"), new ArrayList<>(values.getNonExistentKeys(Arrays.asList("name", "missing"))));

        Values complete = Values.of("id", "device-1", "name", "demo", "version", "v1");
        assertEquals(3, complete.getAllValues().size());
        assertEquals("v1", complete.getAllValues().get("version"));
        assertEquals("parent", complete.merge(Values.of(Collections.singletonMap("parent", "parent")))
                                       .getString("parent", (String) null));
    }

    @Test
    public void testDuplicateKeysKeepLastNonNullValue() {
        Values two = Values.of("id", "old", "id", null);
        Values three = Values.of("id", "old", "other", "present", "id", "new");
        assertEquals("old", two.getString("id", (String) null));
        assertEquals(1, two.size());
        assertEquals("new", three.getString("id", (String) null));
        assertEquals(2, three.size());
    }


}
