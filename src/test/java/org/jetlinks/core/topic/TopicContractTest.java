package org.jetlinks.core.topic;

import org.jetlinks.core.lang.SharedPathString;
import org.junit.Assert;
import org.junit.Test;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TopicContractTest {

    @Test
    @SuppressWarnings("deprecation")
    public void testSubscriberLifecycle() {
        Topic<String> root = Topic.createRoot();
        Topic<String> topic = root.append("/device/001");

        Assert.assertFalse(root.subscribed("subscriber"));
        Assert.assertTrue(root.getSubscribers().isEmpty());
        root.unsubscribe(ignore -> true);
        root.unsubscribeAll();

        AtomicInteger supplierCalls = new AtomicInteger();
        Assert.assertEquals("lazy", topic.getSubscriberOrSubscribe(() -> {
            supplierCalls.incrementAndGet();
            return "lazy";
        }));
        Assert.assertEquals("lazy", topic.getSubscriberOrSubscribe(() -> {
            supplierCalls.incrementAndGet();
            return "unused";
        }));
        Assert.assertEquals(1, supplierCalls.get());

        topic.unsubscribeAll();
        topic.subscribe("repeat");
        topic.subscribe("repeat");
        topic.subscribe("single");
        Assert.assertEquals(List.of("single"), topic.unsubscribe("repeat", "single"));
        Assert.assertTrue(topic.subscribed("repeat"));
        Assert.assertTrue(topic.unsubscribe0("repeat"));
        Assert.assertFalse(topic.subscribed("repeat"));

        topic.subscribe0("replace", false);
        topic.subscribe0("replace", true);
        Assert.assertTrue(topic.unsubscribe0("replace", true));
        Assert.assertFalse(topic.unsubscribe0("replace", true));

        topic.subscribe("keep", "remove");
        topic.unsubscribe("remove"::equals);
        Assert.assertEquals(Set.of("keep"), topic.getSubscribers());
        Assert.assertTrue(topic.unsubscribe0("missing"));
        Assert.assertEquals(1, root.getTotalSubscriber());
    }

    @Test
    public void testPathAndSeparatedSequenceContract() {
        Topic<String> root = Topic.createRoot();
        Assert.assertSame(root, root.append((String) null));
        Assert.assertSame(root, root.append(""));
        Assert.assertSame(root, root.append("/"));
        Assert.assertSame(root, root.append((String[]) null));
        Assert.assertSame(root, root.append(new String[0]));

        Topic<String> topic = root.append(new String[]{"", "device", "001"});
        Assert.assertEquals("/device/001", topic.getTopic());
        Assert.assertArrayEquals(new String[]{"", "device", "001"}, topic.asStringArray());
        Assert.assertEquals('/', topic.separator());
        Assert.assertEquals(3, topic.size());
        Assert.assertEquals("", topic.get(0));
        Assert.assertEquals("device", topic.get(1));
        Assert.assertEquals("001", topic.get(2));
        Assert.assertEquals(9, topic.length());
        Assert.assertSame(topic, topic.intern());
        Assert.assertTrue(topic.toString().contains("/device/001"));

        assertThrows(StringIndexOutOfBoundsException.class, () -> topic.get(3));
        assertThrows(UnsupportedOperationException.class, () -> topic.replace(1, "new"));
        assertThrows(UnsupportedOperationException.class, () -> topic.append('x'));
        assertThrows(UnsupportedOperationException.class, () -> topic.append((CharSequence) "x"));
        assertThrows(UnsupportedOperationException.class,
                     () -> topic.append(new CharSequence[]{"x", "y"}));
        assertThrows(UnsupportedOperationException.class,
                     () -> topic.append((CharSequence) "xyz", 0, 1));
        assertThrows(UnsupportedOperationException.class, () -> topic.range(0, 1));
        assertThrows(UnsupportedOperationException.class, () -> topic.charAt(0));
        assertThrows(UnsupportedOperationException.class, () -> topic.subSequence(0, 1));

        Topic<String> equal = Topic.<String>createRoot().append("/device/001");
        Topic<String> sibling = Topic.<String>createRoot().append("/device/002");
        Topic<String> shallower = Topic.<String>createRoot().append("/device");
        Assert.assertEquals(0, topic.compareTo(topic));
        Assert.assertEquals(0, topic.compareTo(equal));
        Assert.assertNotEquals(0, topic.compareTo(sibling));
        Assert.assertTrue(topic.compareTo(shallower) < 0);
        Assert.assertEquals(0, topic.compareTo(SharedPathString.of("/device/001")));
        Assert.assertEquals(topic, equal);
        Assert.assertNotEquals(topic, sibling);
        Assert.assertNotEquals(topic, "device/001");
        Assert.assertEquals(topic.hashCode(), equal.hashCode());
    }

    @Test
    public void testTraversalViewAndCleanupCallback() {
        Topic<String> root = Topic.createRoot();
        root.append("/device/001").subscribe("device-1");
        root.append("/device/002");
        root.append("/gateway/001").subscribe("gateway-1");

        Assert.assertEquals(5, root.getTotalTopic());
        Assert.assertEquals(2, root.getTotalSubscriber());

        Set<String> traversed = root
            .getAllSubscriber()
            .map(Topic::getTopic)
            .collect(Collectors.toSet())
            .block();
        Assert.assertNotNull(traversed);
        Assert.assertEquals(5, traversed.size());
        Assert.assertTrue(traversed.contains("/device/001"));
        StepVerifier
            .create(root.getAllSubscriber().take(1))
            .expectNextCount(1)
            .verifyComplete();

        TopicView view = root.view();
        Assert.assertEquals("", view.getPart());
        Assert.assertNotNull(view.getChildren());
        Assert.assertEquals(2, view.getChildren().size());
        TopicView deviceView = view
            .getChildren()
            .stream()
            .filter(child -> "device".equals(child.getPart()))
            .findFirst()
            .orElseThrow();
        Assert.assertNotNull(deviceView.getChildren());

        List<Boolean> cleanupStates = new ArrayList<>();
        Assert.assertFalse(root.cleanup((cleaned, topic) -> cleanupStates.add(cleaned)));
        Assert.assertTrue(cleanupStates.contains(Boolean.TRUE));
        Assert.assertTrue(cleanupStates.contains(Boolean.FALSE));
        Assert.assertTrue(root.getTopic("/device/002").isEmpty());

        root.clean();
        Assert.assertEquals(0, root.getTotalTopic());
        Assert.assertEquals(0, root.getTotalSubscriber());
        Assert.assertTrue(root.getChildren().isEmpty());
    }

    private static void assertThrows(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
            Assert.fail("expected " + type.getSimpleName());
        } catch (Throwable error) {
            Assert.assertTrue("unexpected " + error, type.isInstance(error));
        }
    }
}
