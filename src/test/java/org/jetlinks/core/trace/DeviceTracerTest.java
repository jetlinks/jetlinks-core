package org.jetlinks.core.trace;

import org.junit.Test;

import static org.junit.Assert.*;

public class DeviceTracerTest {


    @Test
    public void testSpanName(){
        assertEquals("/device/1/auth", DeviceTracer.SpanName.auth0("1").toString());
        assertEquals("/device/1/decode", DeviceTracer.SpanName.decode0("1").toString());

    }

}