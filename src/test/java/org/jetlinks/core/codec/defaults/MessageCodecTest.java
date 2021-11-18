package org.jetlinks.core.codec.defaults;

import org.jetlinks.core.Payload;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.junit.Test;

import static org.junit.Assert.*;

public class MessageCodecTest {


    @Test
    public void test() {
        MessageCodec codec = MessageCodec.INSTANCE;
        ReadPropertyMessage message = new ReadPropertyMessage();
        message.addHeader("test", "1234");

        Payload payload = codec.encode(message);

        Message msg = codec.decode(payload);

        assertNotNull(msg);
        assertTrue(msg instanceof ReadPropertyMessage);
        assertEquals("1234",msg.getHeaderOrElse("test",null));
        System.out.println(payload.bodyToJson());
    }
}