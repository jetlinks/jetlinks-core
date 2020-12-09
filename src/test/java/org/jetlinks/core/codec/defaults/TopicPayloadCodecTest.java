package org.jetlinks.core.codec.defaults;

import io.netty.buffer.Unpooled;
import org.jetlinks.core.NativePayload;
import org.jetlinks.core.Payload;
import org.jetlinks.core.event.TopicPayload;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

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

    @Test
    public void testNative() {

        TopicPayload topicPayload = TopicPayload.of("/test", NativePayload.of("hello",StringCodec.UTF8));

        Payload payload = TopicPayloadCodec.INSTANCE.encode(topicPayload);

        TopicPayload decode = TopicPayloadCodec.INSTANCE.decode(payload);

        Assert.assertNotNull(decode);
        Assert.assertEquals(decode.getTopic(),topicPayload.getTopic());
        Assert.assertEquals(decode.getBody().toString(StandardCharsets.UTF_8),"hello");
        decode = TopicPayloadCodec.INSTANCE.decode(payload);
        Assert.assertNotNull(decode);
        Assert.assertEquals(decode.getTopic(),topicPayload.getTopic());
        Assert.assertEquals(decode.getBody().toString(StandardCharsets.UTF_8),"hello");
    }

    @Test
    public void testRelease(){
        TopicPayload.POOL_ENABLED=true;
        TopicPayload topicPayload = TopicPayload.of("/test", NativePayload.of("hello",StringCodec.UTF8));
        Assert.assertEquals(topicPayload.refCnt(),1);

        topicPayload.release();
        Assert.assertEquals(topicPayload.refCnt(),0);
        Assert.assertNull(topicPayload.getPayload());

    }
}