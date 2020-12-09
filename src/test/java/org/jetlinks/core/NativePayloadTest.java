package org.jetlinks.core;

import org.jetlinks.core.codec.defaults.StringCodec;
import org.junit.Test;

import static org.junit.Assert.*;

public class NativePayloadTest {


    @Test
    public void testRelease() {
        NativePayload<String> payload = NativePayload.of("hello", StringCodec.UTF8);
        assertEquals(payload.refCnt(), 1);
        payload.retain();
        assertEquals(payload.refCnt(), 2);
        payload.release();

        assertNotNull(payload.getBody());
        assertEquals(payload.refCnt(), 1);
        assertTrue(payload.release());
        assertNull(payload.getNativeObject());
    }

}