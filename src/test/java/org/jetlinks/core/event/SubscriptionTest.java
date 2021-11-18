package org.jetlinks.core.event;

import lombok.SneakyThrows;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.*;

public class SubscriptionTest {

    @Test
    @SneakyThrows
    public void testReadWrite() {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try (ObjectOutputStream out=new ObjectOutputStream(stream)){
            Subscription.builder()
                        .topics("/test")
                        .subscriberId("test")
                        .justBroker()
                        .build()
                        .writeExternal(out);
        }

        byte[] data = stream.toByteArray();

        Subscription subscription = new Subscription();
        subscription.readExternal(new ObjectInputStream(new ByteArrayInputStream(data)));

        assertEquals(subscription.getSubscriber(),"test");
        assertArrayEquals(subscription.getTopics(),new String[]{"/test"});
        assertArrayEquals(subscription.getFeatures(),new Subscription.Feature[]{Subscription.Feature.broker});
    }
}