package org.jetlinks.core.topic;

import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TopicFinderOverloadTest {

    private Topic<String> root;

    @Before
    public void init() {
        root = Topic.createRoot();
        root.append("/device/001/message/property/report").subscribe("exact");
        root.append("/device/002/message/property/report").subscribe("exact-2");
        root.append("/device/*/message/property/report").subscribe("star");
        root.append("/device/**").subscribe("double-star");
        root.append("/device/**/event/*").subscribe("event");
    }

    @Test
    public void testCharSequenceOverloads() {
        for (String path : List.of("/device/001/message/property/report",
                                   "device/001/message/property/report")) {
            SeparatedCharSequence separated = SharedPathString.of(path);
            CharSequence plain = new StringBuilder(path);

            assertZeroArgumentOverload(separated);
            assertZeroArgumentOverload(plain);
            assertTwoArgumentOverload(separated);
            assertTwoArgumentOverload(plain);
            assertOneArgumentOverload(separated);
            assertOneArgumentOverload(plain);
            assertFourArgumentOverload(separated);
            assertFourArgumentOverload(plain);
        }
    }

    @Test
    public void testArrayOverloadWithAndWithoutLeadingSeparator() {
        for (String[] parts : List.of(
            new String[]{"", "device", "001", "message", "property", "report"},
            new String[]{"device", "001", "message", "property", "report"})) {
            List<Topic<String>> matched = new ArrayList<>();
            TopicFinder.find(root, parts, null, null, null, null,
                             (a, b, c, d, found) -> matched.add(found),
                             (a, b, c, d) -> {
                             });
            Assert.assertTrue(paths(matched).contains("/device/001/message/property/report"));
        }
    }

    @Test
    public void testSeparatedWildcardSearchMatrix() {
        assertPaths(SharedPathString.of("/device/*/message/property/report"),
                    "/device/001/message/property/report",
                    "/device/002/message/property/report",
                    "/device/*/message/property/report",
                    "/device/**");
        assertPaths(SharedPathString.of("/device/**"),
                    "/device",
                    "/device/001",
                    "/device/002",
                    "/device/*",
                    "/device/**",
                    "/device/001/message",
                    "/device/002/message",
                    "/device/*/message",
                    "/device/001/message/property",
                    "/device/002/message/property",
                    "/device/*/message/property",
                    "/device/001/message/property/report",
                    "/device/002/message/property/report",
                    "/device/*/message/property/report",
                    "/device/**/event",
                    "/device/**/event/*");
        assertPaths(SharedPathString.of("/device/001/message/event/alarm"),
                    "/device/**",
                    "/device/**/event/*");
    }

    @Test
    public void testNonStringSeparatedPartAndStaticTopicWrappers() {
        SeparatedCharSequence topic = SharedPathString
            .of("/device/001/message/property/report")
            .replace(2, new StringBuilder("001"));

        List<Topic<String>> matched = new ArrayList<>();
        AtomicInteger ended = new AtomicInteger();
        Topic.find(topic, root, "a", "b", "c", "d",
                   (a, b, c, d, found) -> matched.add(found),
                   (a, b, c, d) -> ended.incrementAndGet());
        Assert.assertEquals(1, ended.get());
        Assert.assertTrue(paths(matched).contains("/device/001/message/property/report"));

        matched.clear();
        Topic.find("/device/001/message/property/report", root, "a", "b", "c", "d",
                   (a, b, c, d, found) -> matched.add(found),
                   (a, b, c, d) -> ended.incrementAndGet());
        Assert.assertEquals(2, ended.get());
        Assert.assertTrue(paths(matched).contains("/device/001/message/property/report"));
    }

    @Test
    public void testTopicInstanceFindOverloads() {
        String path = "/device/001/message/property/report";
        Set<String> expected = Set.of(path,
                                      "/device/*/message/property/report",
                                      "/device/**");
        AtomicInteger ended = new AtomicInteger();
        List<Topic<String>> matched = new ArrayList<>();

        root.findTopic((CharSequence) new StringBuilder(path), "a",
                       (a, found) -> {
                           Assert.assertEquals("a", a);
                           matched.add(found);
                       },
                       a -> ended.incrementAndGet());
        Assert.assertEquals(expected, paths(matched));

        matched.clear();
        root.findTopic(SharedPathString.of(path), "a", "b",
                       (a, b, found) -> {
                           Assert.assertEquals("a", a);
                           Assert.assertEquals("b", b);
                           matched.add(found);
                       },
                       (a, b) -> ended.incrementAndGet());
        Assert.assertEquals(expected, paths(matched));

        matched.clear();
        root.findTopic(SharedPathString.of(path), "a", "b", "c", "d",
                       (a, b, c, d, found) -> matched.add(found),
                       (a, b, c, d) -> ended.incrementAndGet());
        Assert.assertEquals(expected, paths(matched));

        matched.clear();
        root.findTopic(path, "a", "b", "c", "d",
                       (a, b, c, d, found) -> matched.add(found),
                       (a, b, c, d) -> ended.incrementAndGet());
        Assert.assertEquals(expected, paths(matched));
        Assert.assertEquals(4, ended.get());
    }

    @Test
    public void testEmptyGenericStringOverload() {
        List<Topic<String>> matched = new ArrayList<>();
        AtomicInteger ended = new AtomicInteger();
        TopicFinder.find(root, "", "a", "b", "c", "d",
                         (a, b, c, d, topic) -> matched.add(topic),
                         (a, b, c, d) -> ended.incrementAndGet());
        Assert.assertEquals(List.of(root), matched);
        Assert.assertEquals(1, ended.get());
    }

    private void assertTwoArgumentOverload(CharSequence topic) {
        List<Topic<String>> matched = new ArrayList<>();
        AtomicInteger ended = new AtomicInteger();
        TopicFinder.find(root, topic, "a", "b",
                         (a, b, found) -> {
                             Assert.assertEquals("a", a);
                             Assert.assertEquals("b", b);
                             matched.add(found);
                         },
                         (a, b) -> ended.incrementAndGet());
        Assert.assertEquals(1, ended.get());
        Assert.assertTrue(paths(matched).contains("/device/001/message/property/report"));
    }

    private void assertZeroArgumentOverload(CharSequence topic) {
        List<Topic<String>> matched = new ArrayList<>();
        AtomicInteger ended = new AtomicInteger();
        TopicFinder.find(root, topic, matched::add, ended::incrementAndGet);
        Assert.assertEquals(1, ended.get());
        Assert.assertTrue(paths(matched).contains("/device/001/message/property/report"));
    }

    private void assertOneArgumentOverload(CharSequence topic) {
        List<Topic<String>> matched = new ArrayList<>();
        AtomicInteger ended = new AtomicInteger();
        TopicFinder.find(root, topic, "context",
                         (context, found) -> {
                             Assert.assertEquals("context", context);
                             matched.add(found);
                         },
                         context -> ended.incrementAndGet());
        Assert.assertEquals(1, ended.get());
        Assert.assertTrue(paths(matched).contains("/device/001/message/property/report"));
    }

    private void assertFourArgumentOverload(CharSequence topic) {
        List<Topic<String>> matched = new ArrayList<>();
        AtomicInteger ended = new AtomicInteger();
        TopicFinder.find(root, topic, "a", "b", "c", "d",
                         (a, b, c, d, found) -> matched.add(found),
                         (a, b, c, d) -> ended.incrementAndGet());
        Assert.assertEquals(1, ended.get());
        Assert.assertTrue(paths(matched).contains("/device/001/message/property/report"));
    }

    private void assertPaths(SeparatedCharSequence topic, String... expected) {
        List<Topic<String>> matched = new ArrayList<>();
        TopicFinder.find(root, topic, matched::add, () -> {
        });
        Assert.assertEquals(Set.of(expected), paths(matched));
        Assert.assertEquals(matched.size(), new HashSet<>(matched).size());
    }

    private static Set<String> paths(List<Topic<String>> topics) {
        return topics
            .stream()
            .map(Topic::getTopic)
            .collect(Collectors.toSet());
    }
}
