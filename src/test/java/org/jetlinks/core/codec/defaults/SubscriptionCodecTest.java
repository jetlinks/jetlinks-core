package org.jetlinks.core.codec.defaults;

import org.jetlinks.core.Payload;
import org.jetlinks.core.event.Subscription;
import org.junit.Test;

import static org.junit.Assert.*;

public class SubscriptionCodecTest {

    @Test
    public void test() {
        SubscriptionCodec codec = new SubscriptionCodec();

        Subscription sub = Subscription.of("test", new String[]{
                "/test", "/test2"
        });

        Payload payload = codec.encode(sub);

        Subscription decode=  codec.decode(payload);

        assertNotNull(decode);
        assertEquals(decode.getSubscriber(),sub.getSubscriber());
        assertArrayEquals(decode.getFeatures(),sub.getFeatures());
        assertArrayEquals(decode.getTopics(),sub.getTopics());



    }
}