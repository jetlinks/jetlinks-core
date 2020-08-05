package org.jetlinks.core.message.codec.context;

import lombok.SneakyThrows;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CodecContextTest {

    @Test
    public void test() {

        CodecContext context = CodecContext.newContext();

        context.cacheDownstream("test", new ReadPropertyMessage(), Duration.ofMinutes(1));

        assertTrue(context.<ReadPropertyMessage>getDownstream("test",false).isPresent());

        context.<ReadPropertyMessage>removeDownstream("test")
                .map(ReadPropertyMessage::newReply)
                .get()
                .success(Collections.singletonMap("test","1"));
    }

    @Test
    @SneakyThrows
    public void testExpire() {

        CodecContext context = CodecContext.newContext();

        for (int i = 0; i < 1000; i++) {
            context.cacheDownstream("test"+i, new ReadPropertyMessage(), Duration.ofSeconds(1));
        }
        Thread.sleep(1000);
        assertFalse(context.<ReadPropertyMessage>removeDownstream("test1").isPresent());


    }

}