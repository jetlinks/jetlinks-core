package org.jetlinks.core.metadata;

import org.jetlinks.core.config.ConfigKey;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class MetadataExpandTest {

    @Test
    public void nullableExpandsAndConvertedValuesKeepTheirSemantics() {
        SimplePropertyMetadata metadata = new SimplePropertyMetadata();
        AtomicInteger conversions = new AtomicInteger();
        ConfigKey<Integer> key = new ConfigKey<Integer>() {
            @Override
            public String getKey() {
                return "value";
            }

            @Override
            public Integer convertValue(Object value) {
                conversions.incrementAndGet();
                return "empty".equals(value) ? null : Integer.valueOf(value.toString());
            }
        };
        assertFalse(metadata.getExpand("value").isPresent());
        assertFalse(metadata.getExpand(key).isPresent());
        Map<String, Object> expands = new HashMap<>();
        metadata.setExpands(expands);
        expands.put("value", null);
        assertFalse(metadata.getExpand(key).isPresent());
        assertEquals(0, conversions.get());
        expands.put("value", "42");
        assertEquals("42", metadata.getExpand("value").get());
        assertEquals(Integer.valueOf(42), metadata.getExpand(key).get());
        expands.put("value", "empty");
        assertFalse(metadata.getExpand(key).isPresent());
    }

    @Test(expected = NumberFormatException.class)
    public void conversionFailureIsNotSuppressed() {
        SimplePropertyMetadata metadata = new SimplePropertyMetadata();
        metadata.setExpands(Map.of("value", "invalid"));
        metadata.getExpand(new ConfigKey<Integer>() {
            @Override
            public String getKey() {
                return "value";
            }

            @Override
            public Integer convertValue(Object value) {
                return Integer.valueOf(value.toString());
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void nullTypedKeyRemainsInvalidEvenWithoutExpands() {
        new SimplePropertyMetadata().getExpand((ConfigKey<?>) null);
    }
}
