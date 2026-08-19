package org.jetlinks.core.topic;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.utils.TopicUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Slf4j
public class TopicFinderTest {

    private Topic<String> root;

    /**
     * 收集 find 匹配到的所有 Topic 节点（用于断言）.
     */
    private static List<Topic<String>> collectMatched(Topic<String> root, String searchTopic) {
        List<Topic<String>> list = new ArrayList<>();
        TopicFinder.find(root, searchTopic, list::add, () -> {
        });
        return list;
    }

    private static List<Topic<String>> collectMatched(Topic<String> root, SeparatedCharSequence searchTopic) {
        List<Topic<String>> list = new ArrayList<>();
        TopicFinder.find(root, searchTopic, list::add, () -> {
        });
        return list;
    }

    /**
     * 返回匹配到的 topic 路径字符串集合，便于断言.
     */
    private static Set<String> matchedPaths(Topic<String> root, String searchTopic) {
        return collectMatched(root, searchTopic).stream()
                                                .map(Topic::getTopic)
                                                .collect(Collectors.toSet());
    }

    private static Set<String> matchedSubscriberPaths(Topic<String> root, CharSequence searchTopic) {
        List<Topic<String>> matched = searchTopic instanceof SeparatedCharSequence
            ? collectMatched(root, (SeparatedCharSequence) searchTopic)
            : collectMatched(root, searchTopic.toString());
        return matched
            .stream()
            .filter(topic -> !topic.getSubscribers().isEmpty())
            .map(Topic::getTopic)
            .collect(Collectors.toSet());
    }

    @Before
    public void init() {
        root = Topic.createRoot();
    }

    @Test
    public void testNull() {
        root.append("/**");

        root.findTopic(
            SharedPathString
                .of("/device/test")
                .replace(0, null),
            topic -> {
            }, () -> {
            });
    }

    // ========== 精确匹配 ==========

    @Test
    public void testExactMatchSingleLevel() {
        root.append("device").subscribe("s1");
        Set<String> matched = matchedPaths(root, "device");
        Assert.assertEquals(Set.of("/device"), matched);
    }

    @Test
    public void testExactMatchMultiLevel() {
        root.append("device").append("001").subscribe("s1");
        Set<String> matched = matchedPaths(root, "device/001");
        Assert.assertEquals(Set.of("/device/001"), matched);
    }

    @Test
    public void testExactMatchNoLeadingSlash() {
        root.append("a").append("b").append("c").subscribe("s1");
        Assert.assertEquals(Set.of("/a/b/c"), matchedPaths(root, "a/b/c"));
    }

    @Test
    public void testExactMatchWithLeadingSlash() {
        root.append("device").append("001").subscribe("s1");
        Set<String> matched = matchedPaths(root, "/device/001");
        Assert.assertEquals(Set.of("/device/001"), matched);
    }

    @Test
    public void testExactMatchNotFound() {
        root.append("device").append("001").subscribe("s1");
        Set<String> matched = matchedPaths(root, "device/002");
        Assert.assertTrue(matched.isEmpty());
    }

    @Test
    public void testExactMatchPrefixNotMatch() {
        root.append("device").append("001").subscribe("s1");
        // 仅有 device/001 时，device/002 与 device/001/extra 均不匹配（device 会匹配到中间节点 /device）
        Assert.assertTrue(matchedPaths(root, "device/002").isEmpty());
        Assert.assertTrue(matchedPaths(root, "device/001/extra").isEmpty());
    }

    // ========== 单层通配符 * ==========

    @Test
    public void testSingleLevelWildcardStar() {
        root.append("device").append("*").subscribe("s1");
        root.append("device").append("001").subscribe("s2");
        Set<String> matched = matchedPaths(root, "device/001");
        Assert.assertEquals(Set.of("/device/001", "/device/*"), matched);
    }

    @Test
    public void testSingleLevelWildcardStarOnly() {
        root.append("device").append("*").subscribe("s1");
        Set<String> matched = matchedPaths(root, "device/001");
        Assert.assertEquals(Set.of("/device/*"), matched);
    }

    @Test
    public void testSingleLevelWildcardStarMultipleSegments() {
        root.append("a").append("*").append("c").subscribe("s1");
        root.append("a").append("b").append("c").subscribe("s2");
        Set<String> matched = matchedPaths(root, "a/b/c");
        Assert.assertEquals(Set.of("/a/b/c", "/a/*/c"), matched);
    }

    @Test
    public void testSingleLevelWildcardStarAtFirstLevel() {
        root.append("*").append("metrics").subscribe("s1");
        root.append("device").append("metrics").subscribe("s2");
        Set<String> matched = matchedPaths(root, "device/metrics");
        Assert.assertEquals(Set.of("/device/metrics", "/*/metrics"), matched);
    }

    // ========== 多层通配符 ** ==========

    @Test
    public void testMultiLevelWildcardDoubleStar() {
        root.append("device").append("**").subscribe("s1");
        Set<String> matched = matchedPaths(root, "device/001");
        Assert.assertEquals(Set.of("/device/**"), matched);
    }

    @Test
    public void testMultiLevelWildcardDoubleStarDeep() {
        root.append("device").append("**").subscribe("s1");
        Set<String> matched = matchedPaths(root, "device/001/event/temperature");
        Assert.assertEquals(Set.of("/device/**"), matched);
    }

    @Test
    public void testMultiLevelWildcardDoubleStarAtRoot() {
        root.append("**").subscribe("s1");
        Set<String> matched = matchedPaths(root, "device/001");
        Assert.assertEquals(Set.of("/**"), matched);
    }

    @Test
    public void testMultiLevelWildcardDoubleStarConsumeZero() {
        root.append("device").append("**").subscribe("s1");
        Set<String> matched = matchedPaths(root, "device");
        // 搜索 "device" 时：精确匹配到 /device，** 消费 0 段也匹配到 /device/**
        Assert.assertEquals(Set.of("/device", "/device/**"), matched);
    }

    @Test
    public void testSearchTopicContainsDoubleStar() {
        root.append("device").append("001").subscribe("s1");
        root.append("device").append("002").subscribe("s2");
        root.append("gateway").append("g1").subscribe("s3");
        Set<String> matched = matchedPaths(root, "device/**");
        // ** 可匹配 0 段，故 /device 也会被匹配
        Assert.assertEquals(Set.of("/device", "/device/001", "/device/002"), matched);
    }

    @Test
    public void testSearchTopicDoubleStarMiddle() {
        root.append("a").append("b").append("c").subscribe("s1");
        root.append("a").append("x").append("c").subscribe("s2");
        root.append("a").append("b").append("x").append("c").subscribe("s3");
        Set<String> matched = matchedPaths(root, "a/**/c");
        Assert.assertTrue(matched.contains("/a/b/c"));
        Assert.assertTrue(matched.contains("/a/x/c"));
        Assert.assertTrue(matched.contains("/a/b/x/c"));
    }

    // ========== 混合：精确 + * + ** ==========

    @Test
    public void testMixedExactAndWildcards() {
        root.append("device").append("001").subscribe("s1");
        root.append("device").append("*").subscribe("s2");
        root.append("device").append("**").subscribe("s3");
        Set<String> matched = matchedPaths(root, "device/001");
        Assert.assertEquals(Set.of("/device/001", "/device/*", "/device/**"), matched);
    }

    @Test
    public void testExactSearchShouldStillTraverseWildcardSubscribers() {
        root.append("device").append("001").subscribe("exact");
        root.append("device").append("*").subscribe("star");
        root.append("device").append("**").subscribe("dstar");

        Set<String> matched = matchedPaths(root, "device/001");
        Assert.assertEquals(Set.of("/device/001", "/device/*", "/device/**"), matched);
    }

    @Test
    public void testExactSearchReferenceMatrix() {
        List<String> patterns = List.of(
            "/tenant/alpha/device/001/message/property/report",
            "/tenant/*/device/*/message/property/report",
            "/tenant/**",
            "/**/message/property/report",
            "/tenant/alpha/device/**",
            "/tenant/alpha/**/property/*"
        );
        List<String> topics = List.of(
            "/tenant/alpha/device/001/message/property/report",
            "/tenant/beta/device/002/message/property/report",
            "/tenant/alpha/device/001/message/event/alarm",
            "/tenant/alpha/gateway/001/online",
            "/other/alpha/device/001/message/property/report"
        );
        patterns.forEach(pattern -> root.append(pattern).subscribe(pattern));

        for (String topic : topics) {
            Set<String> expected = patterns
                .stream()
                .filter(pattern -> TopicUtils.match(pattern, topic))
                .collect(Collectors.toCollection(LinkedHashSet::new));

            Assert.assertEquals(topic, expected, matchedSubscriberPaths(root, topic));
            Assert.assertEquals(topic,
                                expected,
                                matchedSubscriberPaths(root, SharedPathString.of(topic)));
        }
    }

    @Test
    public void testWildcardCacheLifecycle() {
        Topic<String> exact = root.append("/device/001");
        Topic<String> star = root.append("/device/*");
        Topic<String> doubleStar = root.append("/device/**");
        exact.subscribe("exact");
        star.subscribe("star");
        doubleStar.subscribe("double-star");

        Assert.assertEquals(Set.of("/device/001", "/device/*", "/device/**"),
                            matchedPaths(root, "/device/001"));

        star.unsubscribe("star");
        doubleStar.unsubscribe("double-star");
        root.cleanup();

        Assert.assertEquals(Set.of("/device/001"), matchedPaths(root, "/device/001"));
        Assert.assertTrue(root.getTopic("/device/*").isEmpty());
        Assert.assertTrue(root.getTopic("/device/**").isEmpty());

        root.append("/device/*").subscribe("star-recreated");
        root.append("/device/**").subscribe("double-star-recreated");
        Assert.assertEquals(Set.of("/device/001", "/device/*", "/device/**"),
                            matchedPaths(root, "/device/001"));

        root.clean();
        Assert.assertEquals(0, root.getTotalTopic());
        root.append("/device/**").subscribe("double-star-after-clean");
        Assert.assertEquals(Set.of("/device/**"), matchedPaths(root, "/device/001"));
    }

    @Test
    public void testExactSearchDoesNotEmitSameNodeTwice() {
        root.append("/device/001/message/property/report").subscribe("exact");
        root.append("/device/*/message/property/report").subscribe("star");
        root.append("/device/**").subscribe("double-star");

        List<Topic<String>> matched = collectMatched(root, "/device/001/message/property/report");
        Assert.assertEquals(new HashSet<>(matched).size(), matched.size());
    }

    @Test
    public void testLargeDeduplicationSetDoesNotRemainRetained() {
        TopicFinder.ReusableTopicSet reusable = new TopicFinder.ReusableTopicSet();
        Set<Topic<String>> large = reusable.values();
        for (int i = 0; i < 5_000; i++) {
            large.add(Topic.createRoot());
        }

        reusable.reset();
        Set<Topic<String>> compact = reusable.values();
        Assert.assertNotSame(large, compact);
        Assert.assertTrue(compact.isEmpty());

        compact.add(root);
        reusable.reset();
        Assert.assertSame(compact, reusable.<String>values());
        Assert.assertTrue(compact.isEmpty());
    }

    @Test
    public void testMixedStarAndDoubleStarInTree() {
        root.append("a").append("*").append("c").subscribe("s1");
        root.append("a").append("**").subscribe("s2");
        Set<String> matched = matchedPaths(root, "a/b/c");
        Assert.assertTrue(matched.contains("/a/*/c"));
        Assert.assertTrue(matched.contains("/a/**"));
    }

    // ========== 多分支 ==========

    @Test
    public void testMultipleBranches() {
        root.append("device").append("001").subscribe("s1");
        root.append("device").append("002").subscribe("s2");
        root.append("gateway").append("g1").subscribe("s3");
        Set<String> matched = matchedPaths(root, "device/001");
        Assert.assertEquals(Set.of("/device/001"), matched);
        matched = matchedPaths(root, "gateway/g1");
        Assert.assertEquals(Set.of("/gateway/g1"), matched);
    }

    @Test
    public void testWildcardMatchesMultipleBranches() {
        root.append("device").append("*").subscribe("s1");
        root.append("device").append("001").subscribe("s2");
        root.append("device").append("002").subscribe("s3");
        Set<String> matched = matchedPaths(root, "device/001");
        Assert.assertEquals(Set.of("/device/001", "/device/*"), matched);
        matched = matchedPaths(root, "device/002");
        Assert.assertEquals(Set.of("/device/002", "/device/*"), matched);
    }

    // ========== 空与边界 ==========

    @Test
    public void testEmptySearchTopic() {
        root.append("device").subscribe("s1");
        List<Topic<String>> matched = collectMatched(root, "");
        Assert.assertTrue(matched.size() <= 1);
        if (!matched.isEmpty()) {
            Assert.assertEquals(root, matched.get(0));
        }
    }

    @Test
    public void testRootOnly() {
        List<Topic<String>> matched = collectMatched(root, "");
        Assert.assertFalse(matched.isEmpty());
        Assert.assertEquals(root, matched.get(0));
    }

    @Test
    public void testNoDuplicateWhenDoubleStarInSearch() {
        root.append("a").append("**").append("c").subscribe("s1");
        root.append("a").append("b").append("c").subscribe("s2");
        List<Topic<String>> matched = collectMatched(root, "a/**/c");
        Set<Topic<String>> unique = new HashSet<>(matched);
        Assert.assertEquals("** 搜索时同一节点不应重复", unique.size(), matched.size());
    }

    // ========== 重载：CharSequence、带参数、end 回调 ==========

    @Test
    public void testFindWithCharSequence() {
        root.append("device").append("001").subscribe("s1");
        List<Topic<String>> list = new ArrayList<>();
        TopicFinder.find(root, (CharSequence) "device/001", list::add, () -> {
        });
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("/device/001", list.get(0).getTopic());
    }

    @Test
    public void testFindWithSeparatedCharSequence() {
        root.append("device").append("001").subscribe("s1");
        List<Topic<String>> list = new ArrayList<>();
        // TopicFinder 内部从 idx=1 开始，期望 parts[0] 为空（根），故使用带前导 / 的路径
        SeparatedCharSequence seq = SharedPathString.of("/device/001");
        TopicFinder.find(root, seq, list::add, () -> {
        });
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("/device/001", list.get(0).getTopic());
    }

    @Test
    public void testFindWithArgsAndEndCallback() {
        root.append("device").append("001").subscribe("s1");
        List<Topic<String>> collected = new ArrayList<>();
        AtomicInteger endCallCount = new AtomicInteger(0);
        TopicFinder.find(root, "device/001",
                         "arg0", "arg1", "arg2", "arg3",
                         (a0, a1, a2, a3, topic) -> {
                             Assert.assertEquals("arg0", a0);
                             Assert.assertEquals("arg1", a1);
                             collected.add(topic);
                         },
                         (a0, a1, a2, a3) -> {
                             Assert.assertEquals("arg0", a0);
                             Assert.assertEquals("arg1", a1);
                             endCallCount.incrementAndGet();
                         });
        Assert.assertEquals(1, collected.size());
        Assert.assertEquals(1, endCallCount.get());
    }

    @Test
    public void testFindWithTwoArgsAndEndCallback() {
        root.append("device").append("*").subscribe("s1");
        List<Topic<String>> collected = new ArrayList<>();
        AtomicInteger endCalls = new AtomicInteger(0);
        TopicFinder.find(root, "device/001",
                         "A", "B",
                         (a, b, topic) -> collected.add(topic),
                         (a, b) -> endCalls.incrementAndGet());
        Assert.assertTrue(collected.size() >= 1);
        Assert.assertEquals(1, endCalls.get());
    }

    @Test
    public void testFindWithStringArrayOverload() {
        root.append("device").append("001").subscribe("s1");
        List<Topic<String>> list = new ArrayList<>();
        TopicFinder.find(root, new String[]{"", "device", "001"}, null, null, null, null,
                         (a, b, c, d, t) -> list.add(t), (a, b, c, d) -> {
            });
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("/device/001", list.get(0).getTopic());
    }

    // ========== 深层 ** 与 * 组合 ==========

    @Test
    public void testDoubleStarThenExact() {
        root.append("a").append("**").append("c").subscribe("s1");
        root.append("a").append("b").append("c").subscribe("s2");
        root.append("a").append("x").append("y").append("c").subscribe("s3");
        Set<String> matched = matchedPaths(root, "a/b/c");
        Assert.assertTrue(matched.contains("/a/b/c"));
        Assert.assertTrue(matched.contains("/a/**/c"));
        matched = matchedPaths(root, "a/x/y/c");
        Assert.assertTrue(matched.contains("/a/x/y/c"));
        Assert.assertTrue(matched.contains("/a/**/c"));
    }

    @Test
    public void testStarThenDoubleStar() {
        root.append("*").append("**").subscribe("s1");
        root.append("device").append("001").subscribe("s2");
        Set<String> matched = matchedPaths(root, "device/001");
        Assert.assertTrue(matched.contains("/device/001"));
        Assert.assertTrue(matched.contains("/*/**"));
    }

    // ========== 订阅含通配符 + 推送也含通配符 ==========

    @Test
    public void testSubscribeAndPushBothWithWildcards0() {
        root.append("device/*/*/message").subscribe("all");

        List<Topic<String>> topics = collectMatched(root, "/device/*/test/message");

        Assert.assertEquals(1, topics.size());
        Assert.assertEquals("/device/*/*/message", topics.get(0).getTopic());
    }

    @Test
    public void testSubscribeAndPushBothWithWildcards() {
        // 订阅：device/*, device/**, device/**/event, */metrics, *
        root.append("device").append("*").subscribe("s-star");
        root.append("device").append("**").subscribe("s-dstar");           // 显式订阅 device/**
        root.append("device").append("**").append("event").subscribe("s-dstar-event");
        root.append("*").append("metrics").subscribe("s-star-metrics");
        root.append("*").subscribe("s-root-star");
        root.append("device").append("001").subscribe("s-exact");
        root.append("device").append("001").append("event").subscribe("s-exact-event");

        // 推送 device/001 → 应匹配 device/*, device/**, device/001（根下 * 只匹配一层，不匹配 device/001）
        Set<String> matched = matchedPaths(root, "device/001");
        Assert.assertTrue(matched.contains("/device/*"));
        Assert.assertTrue(matched.contains("/device/**"));
        Assert.assertTrue(matched.contains("/device/001"));

        // 推送 device/001/event → 应匹配 device/**/event, device/001/event
        matched = matchedPaths(root, "device/001/event");
        Assert.assertTrue(matched.contains("/device/**/event"));
        Assert.assertTrue(matched.contains("/device/001/event"));

        // 推送 device/metrics → 应匹配 */metrics（* 匹配 device，metrics 匹配下一层）
        matched = matchedPaths(root, "device/metrics");
        Assert.assertTrue(matched.contains("/*/metrics"));

        // 推送 topic 也带通配符：device/* → 匹配 device 下第一层所有订阅节点
        matched = matchedPaths(root, "device/*");
        Assert.assertTrue(matched.contains("/device/*"));
        Assert.assertTrue(matched.contains("/device/001"));
        Assert.assertTrue(matched.contains("/device/**"));

        // 推送 device/** → 匹配 device 及其下所有层级
        matched = matchedPaths(root, "device/**");
        Assert.assertTrue(matched.contains("/device"));
        Assert.assertTrue(matched.contains("/device/*"));
        Assert.assertTrue(matched.contains("/device/**"));
        Assert.assertTrue(matched.contains("/device/001"));
        Assert.assertTrue(matched.contains("/device/001/event"));
        Assert.assertTrue(matched.contains("/device/**/event"));
    }
}
