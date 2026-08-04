package org.jetlinks.core.event;

import lombok.SneakyThrows;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import static org.junit.Assert.*;

public class SubscriptionTest {

    // 固定旧 Externalizable 字节布局，避免新增结构化 Plan 时意外改变已发布协议。
    private static final String LEGACY_SUBSCRIPTION_BYTES =
        "rO0ABXcdAAR0ZXN0AAAAAQAFL3Rlc3QAAAAAAAAABAAAAAA=";

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

    @Test
    @SneakyThrows
    public void testLegacyExternalizableBytes() {
        Subscription subscription = Subscription.builder()
            .topics("/test")
            .subscriberId("test")
            .justBroker()
            .build();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(stream)) {
            subscription.writeExternal(output);
        }

        String encoded = Base64.getEncoder().encodeToString(stream.toByteArray());
        assertEquals(LEGACY_SUBSCRIPTION_BYTES, encoded);

        Subscription restored = new Subscription();
        try (ObjectInputStream input = new ObjectInputStream(
            new ByteArrayInputStream(Base64.getDecoder().decode(LEGACY_SUBSCRIPTION_BYTES)))) {
            restored.readExternal(input);
        }
        assertEquals("test", restored.getSubscriber());
        assertArrayEquals(new String[]{"/test"}, restored.getTopics());
        assertArrayEquals(
            new Subscription.Feature[]{Subscription.Feature.broker},
            restored.getFeatures()
        );
        assertEquals(0, restored.getPriority());
    }
}
