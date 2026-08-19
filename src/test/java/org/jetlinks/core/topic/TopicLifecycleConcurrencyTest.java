package org.jetlinks.core.topic;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TopicLifecycleConcurrencyTest {

    @Test
    public void testSubscribeAndAppendFromNodeRemovedByCleanup() {
        Topic<String> root = Topic.createRoot();
        Topic<String> staleLeaf = root.append("/device/001");
        Topic<String> staleBranch = root.append("/gateway");

        root.cleanup();
        Assert.assertEquals(0, root.getTotalTopic());

        staleLeaf.subscribe("late-device");
        staleBranch.append("001").subscribe("late-gateway");

        Assert.assertEquals(Set.of("late-device"), matchedSubscribers(root, "/device/001"));
        Assert.assertEquals(Set.of("late-gateway"), matchedSubscribers(root, "/gateway/001"));
        Assert.assertTrue(staleLeaf.unsubscribe("late-device").contains("late-device"));
        Assert.assertTrue(matchedSubscribers(root, "/device/001").isEmpty());
        assertWildcardIndex(root);
    }

    @Test
    public void testSubscribeFromDescendantRemovedByClean() {
        Topic<String> root = Topic.createRoot();
        Topic<String> staleLeaf = root.append("/org/001/device/001/message/property/report");
        staleLeaf.subscribe("before-clean");

        root.clean();
        staleLeaf.subscribe("after-clean");

        Assert.assertEquals(Set.of("after-clean"),
                            matchedSubscribers(root,
                                               "/org/001/device/001/message/property/report"));
        assertWildcardIndex(root);
    }

    @Test(timeout = 20_000)
    public void testConcurrentMutationCleanupAndClean() throws Exception {
        Topic<String> root = Topic.createRoot();
        int writerCount = 4;
        int iterations = 3_000;
        ExecutorService executor = Executors.newFixedThreadPool(writerCount + 1);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean running = new AtomicBoolean(true);
        List<Future<?>> writers = new ArrayList<>();

        Future<?> cleaner = executor.submit(() -> {
            await(start);
            int rounds = 0;
            while (running.get()) {
                if ((++rounds & 31) == 0) {
                    root.clean();
                } else {
                    root.cleanup();
                }
            }
        });

        for (int writer = 0; writer < writerCount; writer++) {
            int writerId = writer;
            writers.add(executor.submit(() -> {
                await(start);
                for (int i = 0; i < iterations; i++) {
                    String subscriber = writerId + "-" + i;
                    String prefix = "/org/" + (i & 31) + "/device/" + writerId;
                    Topic<String> exact = root.append(prefix + "/message/property/report");
                    Topic<String> wildcard = root.append(prefix + "/message/**");
                    exact.subscribe(subscriber);
                    wildcard.subscribe(subscriber);
                    exact.unsubscribe(subscriber);
                    wildcard.unsubscribe(subscriber);
                }
            }));
        }

        start.countDown();
        try {
            for (Future<?> writer : writers) {
                writer.get(15, TimeUnit.SECONDS);
            }
        } finally {
            running.set(false);
        }
        cleaner.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        root.clean();
        root.append("/org/001/device/001/message/property/report").subscribe("exact");
        root.append("/org/*/device/*/message/property/report").subscribe("star");
        root.append("/org/**").subscribe("double-star");

        Assert.assertEquals(Set.of("exact", "star", "double-star"),
                            matchedSubscribers(root,
                                               "/org/001/device/001/message/property/report"));
        assertWildcardIndex(root);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }

    private static Set<String> matchedSubscribers(Topic<String> root, String topic) {
        return root
            .findTopic(topic)
            .flatMapIterable(Topic::getSubscribers)
            .collect(Collectors.toSet())
            .block();
    }

    private static void assertWildcardIndex(Topic<?> node) {
        Map<String, ? extends Topic<?>> children = node.getChildrenMap();
        Assert.assertSame(children == null ? null : children.get("*"),
                          node.getStarChild());
        Assert.assertSame(children == null ? null : children.get("**"),
                          node.getDoubleStarChild());
        if (children != null) {
            for (Topic<?> child : children.values()) {
                Assert.assertSame(node, child.getParent());
                assertWildcardIndex(child);
            }
        }
    }
}
