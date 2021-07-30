package org.jetlinks.core.codec.defaults;

import org.jetlinks.core.Payload;
import org.junit.Test;

import static org.junit.Assert.*;

public class ErrorCodecTest {

    @Test
    public void test() {

        Payload payload = ErrorCodec.DEFAULT.encode(new RuntimeException("test"));

        Throwable exception = ErrorCodec.DEFAULT.decode(payload);

        exception.printStackTrace();

    }
}