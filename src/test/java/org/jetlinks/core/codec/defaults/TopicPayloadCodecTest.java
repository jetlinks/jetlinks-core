package org.jetlinks.core.codec.defaults;

import io.netty.buffer.Unpooled;
import org.jetlinks.core.Payload;
import org.jetlinks.core.event.TopicPayload;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class TopicPayloadCodecTest {


    @Test
    public void test() {

        TopicPayload topicPayload = TopicPayload.of("/test", Payload.of(Unpooled.wrappedBuffer("hello".getBytes())));

        Payload payload = TopicPayloadCodec.INSTANCE.encode(topicPayload);

        TopicPayload decode = TopicPayloadCodec.INSTANCE.decode(payload);

        Assert.assertNotNull(decode);
        Assert.assertEquals(decode.getTopic(),topicPayload.getTopic());
        Assert.assertEquals(decode.getBody().toString(StandardCharsets.UTF_8),"hello");


    }

}