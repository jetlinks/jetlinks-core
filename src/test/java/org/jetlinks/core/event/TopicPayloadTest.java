package org.jetlinks.core.event;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class TopicPayloadTest {

    @Test
    public void shouldCacheMaterializedTopicAndKeepOriginalSequence() {
        CountingCharSequence topic = new CountingCharSequence(
            "/org/org-123/device/product-1/device-1/message/property/report"
        );
        TopicPayload payload = TopicPayload.of(topic, "payload", Collections.emptyMap());

        String first = payload.getTopic();
        String second = payload.getTopic();

        assertSame(first, second);
        assertSame(topic, payload.getTopic0());
        assertEquals(1, topic.toStringCount.get());
    }

    @Test
    public void shouldReuseStringTopicReference() {
        String topic = new String("/device/product-1/device-1/online");
        TopicPayload payload = TopicPayload.of(topic, "payload");

        assertSame(topic, payload.getTopic());
        assertSame(topic, payload.getTopic());
        assertSame(topic, payload.getTopic0());
    }

    @Test
    public void shouldReturnCorrectTopicDuringConcurrentFirstRead() throws Exception {
        CountingCharSequence topic = new CountingCharSequence(
            "/org/org-123/device/product-1/device-1/message/event/alarm"
        );
        TopicPayload payload = TopicPayload.of(topic, "payload");
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Callable<String>> tasks = IntStream
                .range(0, 32)
                .mapToObj(ignore -> (Callable<String>) () -> {
                    start.await();
                    return payload.getTopic();
                })
                .collect(Collectors.toList());
            List<Future<String>> results = tasks
                .stream()
                .map(executor::submit)
                .collect(Collectors.toList());

            start.countDown();
            for (Future<String> result : results) {
                assertEquals(topic.value, result.get());
            }
            assertSame(payload.getTopic(), payload.getTopic());
            assertSame(topic, payload.getTopic0());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void shouldRebuildTransientTopicViewAfterExternalization() throws Exception {
        SerializableCountingCharSequence.reset();
        TopicPayload source = TopicPayload.of(
            new SerializableCountingCharSequence(
                "/org/org-123/device/product-1/device-1/message/property/report"
            ),
            "payload"
        );
        assertEquals(source.getTopic(), source.getTopic());
        assertEquals(1, SerializableCountingCharSequence.count());

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(buffer)) {
            output.writeObject(source);
        }

        SerializableCountingCharSequence.reset();
        TopicPayload restored;
        try (ObjectInputStream input = new ObjectInputStream(
            new ByteArrayInputStream(buffer.toByteArray()))) {
            restored = (TopicPayload) input.readObject();
        }

        String first = restored.getTopic();
        assertSame(first, restored.getTopic());
        assertEquals(1, SerializableCountingCharSequence.count());
        assertEquals("payload", restored.decode());
    }

    private static class CountingCharSequence implements CharSequence, Serializable {

        private static final long serialVersionUID = 1L;

        protected final String value;
        private final AtomicInteger toStringCount = new AtomicInteger();

        private CountingCharSequence(String value) {
            this.value = value;
        }

        @Override
        public int length() {
            return value.length();
        }

        @Override
        public char charAt(int index) {
            return value.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return value.subSequence(start, end);
        }

        @Override
        public String toString() {
            toStringCount.incrementAndGet();
            return new String(value);
        }
    }

    private static final class SerializableCountingCharSequence
        extends CountingCharSequence
        implements Serializable {

        private static final long serialVersionUID = 1L;
        private static final AtomicInteger TO_STRING_COUNT = new AtomicInteger();

        private SerializableCountingCharSequence(String value) {
            super(value);
        }

        private static void reset() {
            TO_STRING_COUNT.set(0);
        }

        private static int count() {
            return TO_STRING_COUNT.get();
        }

        @Override
        public String toString() {
            TO_STRING_COUNT.incrementAndGet();
            return new String(super.value);
        }
    }
}
