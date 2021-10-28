package org.jetlinks.core.codec.defaults;

import org.jetlinks.core.things.ThingProperty;
import org.junit.Test;

import static org.junit.Assert.*;

public class ThingPropertyCodecTest {

    @Test
    public void test() {
        ThingProperty property = ThingProperty.of("test", 1, System.currentTimeMillis());

        ThingProperty decode = ThingPropertyCodec.INSTANCE.decode(ThingPropertyCodec.INSTANCE.encode(property));
        assertEquals(decode.getProperty(), property.getProperty());
        assertEquals(decode.getValue(), property.getValue());
        assertEquals(decode.getTimestamp(), property.getTimestamp());
    }
}