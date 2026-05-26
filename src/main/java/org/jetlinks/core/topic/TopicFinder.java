package org.jetlinks.core.topic;

import org.hswebframework.web.recycler.Recyclable;
import org.hswebframework.web.recycler.Recycler;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.utils.TopicUtils;
import reactor.function.Consumer3;
import reactor.function.Consumer4;
import reactor.function.Consumer5;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Topic 搜索工具类，实现基于 DFS 的搜索算法.
 *
 * @author zhouhao
 * @since 1.1.9
 */
public class TopicFinder {

    private static final byte WILDCARD_NONE = 0;
    private static final byte WILDCARD_SINGLE = 1;
    private static final byte WILDCARD_DOUBLE = 2;

    private static final Recycler<Set<Topic<?>>> SHARED_SET =
        Recycler.create(HashSet::new, Collection::clear, 256);

    /**
     * 使用 DFS（深度优先）算法搜索匹配 topic 的节点.
     *
     * @param root  根节点
     * @param topic 搜索路径
     * @param sink  匹配结果接收器
     * @param end   搜索结束回调
     * @param <T>   订阅者类型
     */
    public static <T> void find(Topic<T> root,
                                String topic,
                                Consumer<Topic<T>> sink,
                                Runnable end) {
        if (topic.isEmpty()) {
            sink.accept(root);
            end.run();
            return;
        }

        find(root, splitTopic(topic),
             null, null, null, null,
             (a, b, c, d, t) -> sink.accept(t),
             (a, b, c, d) -> end.run());
    }

    public static <T> void find(Topic<T> root,
                                CharSequence topic,
                                Consumer<Topic<T>> sink,
                                Runnable end) {
        if (topic instanceof SeparatedCharSequence) {
            find(root, (SeparatedCharSequence) topic,
                 null, null, null, null,
                 (a, b, c, d, t) -> sink.accept(t),
                 (a, b, c, d) -> end.run());
        } else {
            find(root, topic.toString(), sink, end);
        }
    }

    public static <T, ARG0, ARG1, ARG2, ARG3> void find(Topic<T> root,
                                                        String topic,
                                                        ARG0 arg0, ARG1 arg1, ARG2 arg2, ARG3 arg3,
                                                        Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink,
                                                        Consumer4<ARG0, ARG1, ARG2, ARG3> end) {
        if (topic.isEmpty()) {
            sink.accept(arg0, arg1, arg2, arg3, root);
            end.accept(arg0, arg1, arg2, arg3);
            return;
        }
        find(root, splitTopic(topic), arg0, arg1, arg2, arg3, sink, end);
    }

    private static String[] splitTopic(String topic) {
        String[] topics = TopicUtils.split(topic, false, false);
        if (topic.charAt(0) != '/') {
            String[] newTopics = new String[topics.length + 1];
            newTopics[0] = "";
            System.arraycopy(topics, 0, newTopics, 1, topics.length);
            topics = newTopics;
        }
        return topics;
    }

    public static <T, A, B> void find(Topic<T> root,
                                      CharSequence topic,
                                      A arg1,
                                      B arg2,
                                      Consumer3<A, B, Topic<T>> sink,
                                      BiConsumer<A, B> end) {
        if (topic instanceof SeparatedCharSequence) {
            find(root, (SeparatedCharSequence) topic, arg1, arg2, null, null,
                 (a1, b, nil2, nil3, _topic) -> sink.accept(a1, b, _topic),
                 (a1, b, nil2, nil3) -> end.accept(a1, b));
        } else {
            find(root, topic.toString(), arg1, arg2, null, null,
                 (a1, b, nil2, nil3, _topic) -> sink.accept(a1, b, _topic),
                 (a1, b, nil2, nil3) -> end.accept(a1, b));
        }
    }

    public static <T, ARG0, ARG1, ARG2, ARG3> void find(Topic<T> root,
                                                        CharSequence topic,
                                                        ARG0 arg0, ARG1 arg1, ARG2 arg2, ARG3 arg3,
                                                        Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink,
                                                        Consumer4<ARG0, ARG1, ARG2, ARG3> end) {
        if (topic instanceof SeparatedCharSequence) {
            find(root, (SeparatedCharSequence) topic, arg0, arg1, arg2, arg3, sink, end);
        } else {
            find(root, topic.toString(), arg0, arg1, arg2, arg3, sink, end);
        }
    }

    public static <T, A> void find(Topic<T> root,
                                   CharSequence topic,
                                   A arg1,
                                   BiConsumer<A, Topic<T>> sink,
                                   Consumer<A> end) {
        if (topic instanceof SeparatedCharSequence) {
            find(root, (SeparatedCharSequence) topic, arg1, null, null, null,
                 (a1, nil1, nil2, nil3, _topic) -> sink.accept(a1, _topic),
                 (a1, nil1, nil2, nil3) -> end.accept(a1));
        } else {
            find(root, TopicUtils.split(topic.toString(), false, false),
                 arg1, null, null, null,
                 (a1, nil1, nil2, nil3, _topic) -> sink.accept(a1, _topic),
                 (a1, nil1, nil2, nil3) -> end.accept(a1));
        }
    }

    public static <T, ARG0, ARG1, ARG2, ARG3> void find(Topic<T> root,
                                                        SeparatedCharSequence topic,
                                                        ARG0 arg0, ARG1 arg1, ARG2 arg2, ARG3 arg3,
                                                        Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink,
                                                        Consumer4<ARG0, ARG1, ARG2, ARG3> end) {
        byte wildcardMode = detectWildcardMode(topic);
        // 精确 topic 直接走无分支路径, 避免通配符 DFS 的额外递归和集合开销.
        if (wildcardMode == WILDCARD_NONE) {
            findExactInner(topic, 1, root, arg0, arg1, arg2, arg3, sink);
            end.accept(arg0, arg1, arg2, arg3);
            return;
        }
        if (wildcardMode == WILDCARD_DOUBLE) {
            Recyclable<Set<Topic<T>>> recyclableSet = (Recyclable) SHARED_SET.take(true);
            try {
                findDFSInner(topic, 1, root, recyclableSet.get(),
                             arg0, arg1, arg2, arg3, sink);
            } finally {
                recyclableSet.recycle();
                end.accept(arg0, arg1, arg2, arg3);
            }
        } else {
            findDFSInner(topic, 1, root, null,
                         arg0, arg1, arg2, arg3, sink);
            end.accept(arg0, arg1, arg2, arg3);
        }
    }

    public static <T, ARG0, ARG1, ARG2, ARG3> void find(Topic<T> root,
                                                        String[] topicParts,
                                                        ARG0 arg0, ARG1 arg1, ARG2 arg2, ARG3 arg3,
                                                        Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink,
                                                        Consumer4<ARG0, ARG1, ARG2, ARG3> end) {

        byte wildcardMode = detectWildcardMode(topicParts);
        // 精确 topic 直接走无分支路径, 避免通配符 DFS 的额外递归和集合开销.
        if (wildcardMode == WILDCARD_NONE) {
            findExactInner(topicParts, 1, root, arg0, arg1, arg2, arg3, sink);
            end.accept(arg0, arg1, arg2, arg3);
            return;
        }
        if (wildcardMode == WILDCARD_DOUBLE) {
            Recyclable<Set<Topic<T>>> recyclableSet = (Recyclable) SHARED_SET.take(true);
            try {
                findDFSInner(topicParts, 1, root, recyclableSet.get(),
                             arg0, arg1, arg2, arg3, sink);
            } finally {
                recyclableSet.recycle();
                end.accept(arg0, arg1, arg2, arg3);
            }
        } else {
            findDFSInner(topicParts, 1, root, null,
                         arg0, arg1, arg2, arg3, sink);
            end.accept(arg0, arg1, arg2, arg3);
        }
    }

    private static byte detectWildcardMode(String[] parts) {
        byte mode = WILDCARD_NONE;
        for (String p : parts) {
            if ("*".equals(p)) {
                mode = WILDCARD_SINGLE;
            } else if ("**".equals(p)) {
                return WILDCARD_DOUBLE;
            }
        }
        return mode;
    }

    private static byte detectWildcardMode(SeparatedCharSequence parts) {
        byte mode = WILDCARD_NONE;
        for (int i = 0, n = parts.size(); i < n; i++) {
            CharSequence c = parts.get(i);
            if (c == null) {
                continue;
            }
            if (c.length() == 1 && c.charAt(0) == '*') {
                mode = WILDCARD_SINGLE;
            } else if (c.length() == 2 && c.charAt(0) == '*' && c.charAt(1) == '*') {
                return WILDCARD_DOUBLE;
            }
        }
        return mode;
    }

    private static String topicPart(CharSequence part) {
        return part instanceof String ? (String) part : String.valueOf(part);
    }

    private static <T, ARG0, ARG1, ARG2, ARG3> void findExactInner(
        final String[] st,
        final int idx,
        final Topic<T> node,
        final ARG0 arg0, final ARG1 arg1, final ARG2 arg2, final ARG3 arg3,
        final Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink) {

        if (idx >= st.length) {
            sink.accept(arg0, arg1, arg2, arg3, node);
            Topic<T> dstar = node.getDoubleStarChild();
            if (dstar != null) {
                findExactInner(st, idx, dstar, arg0, arg1, arg2, arg3, sink);
            }
            return;
        }

        String searchPart = st[idx];

        if ("**".equals(node.getPart())) {
            findExactInner(st, idx + 1, node, arg0, arg1, arg2, arg3, sink);
            Map<String, Topic<T>> ch = node.getChildrenMap();
            if (ch == null) {
                return;
            }
            Topic<T> exact = ch.get(searchPart);
            if (exact != null) {
                findExactInner(st, idx + 1, exact, arg0, arg1, arg2, arg3, sink);
            }
            Topic<T> star = node.getStarChild();
            if (star != null) {
                findExactInner(st, idx + 1, star, arg0, arg1, arg2, arg3, sink);
            }
            Topic<T> innerDstar = node.getDoubleStarChild();
            if (innerDstar != null) {
                findExactInner(st, idx, innerDstar, arg0, arg1, arg2, arg3, sink);
            }
            return;
        }

        Map<String, Topic<T>> children = node.getChildrenMap();
        if (children == null) {
            return;
        }

        Topic<T> exact = children.get(searchPart);
        if (exact != null) {
            findExactInner(st, idx + 1, exact, arg0, arg1, arg2, arg3, sink);
        }
        Topic<T> star = node.getStarChild();
        if (star != null) {
            findExactInner(st, idx + 1, star, arg0, arg1, arg2, arg3, sink);
        }
        Topic<T> dstar = node.getDoubleStarChild();
        if (dstar != null) {
            findExactInner(st, idx, dstar, arg0, arg1, arg2, arg3, sink);
        }
    }

    private static <T, ARG0, ARG1, ARG2, ARG3> void findExactInner(
        final SeparatedCharSequence st,
        final int idx,
        final Topic<T> node,
        final ARG0 arg0, final ARG1 arg1, final ARG2 arg2, final ARG3 arg3,
        final Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink) {

        final int stSize = st.size();

        if (idx >= stSize) {
            sink.accept(arg0, arg1, arg2, arg3, node);
            Topic<T> dstar = node.getDoubleStarChild();
            if (dstar != null) {
                findExactInner(st, idx, dstar, arg0, arg1, arg2, arg3, sink);
            }
            return;
        }

        String searchPart = topicPart(st.get(idx));

        if ("**".equals(node.getPart())) {
            findExactInner(st, idx + 1, node, arg0, arg1, arg2, arg3, sink);
            Map<String, Topic<T>> ch = node.getChildrenMap();
            if (ch == null) {
                return;
            }
            Topic<T> exact = ch.get(searchPart);
            if (exact != null) {
                findExactInner(st, idx + 1, exact, arg0, arg1, arg2, arg3, sink);
            }
            Topic<T> star = node.getStarChild();
            if (star != null) {
                findExactInner(st, idx + 1, star, arg0, arg1, arg2, arg3, sink);
            }
            Topic<T> innerDstar = node.getDoubleStarChild();
            if (innerDstar != null) {
                findExactInner(st, idx, innerDstar, arg0, arg1, arg2, arg3, sink);
            }
            return;
        }

        Map<String, Topic<T>> children = node.getChildrenMap();
        if (children == null) {
            return;
        }

        Topic<T> exact = children.get(searchPart);
        if (exact != null) {
            findExactInner(st, idx + 1, exact, arg0, arg1, arg2, arg3, sink);
        }
        Topic<T> star = node.getStarChild();
        if (star != null) {
            findExactInner(st, idx + 1, star, arg0, arg1, arg2, arg3, sink);
        }
        Topic<T> dstar = node.getDoubleStarChild();
        if (dstar != null) {
            findExactInner(st, idx, dstar, arg0, arg1, arg2, arg3, sink);
        }
    }

    private static <T, ARG0, ARG1, ARG2, ARG3> void findDFSInner(
        final String[] st,
        final int idx,
        final Topic<T> node,
        final Set<Topic<T>> emitted,
        final ARG0 arg0, final ARG1 arg1, final ARG2 arg2, final ARG3 arg3,
        final Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink) {

        if (idx >= st.length) {
            if (emitted == null || emitted.add(node)) {
                sink.accept(arg0, arg1, arg2, arg3, node);
            }
            Topic<T> dstar = node.getDoubleStarChild();
            if (dstar != null) {
                findDFSInner(st, idx, dstar, emitted, arg0, arg1, arg2, arg3, sink);
            }
            return;
        }

        final String searchPart = st[idx];

        if ("**".equals(node.getPart())) {
            findDFSInner(st, idx + 1, node, emitted, arg0, arg1, arg2, arg3, sink);
            Map<String, Topic<T>> ch = node.getChildrenMap();
            if (ch == null) return;
            if ("**".equals(searchPart)) {
                for (Topic<T> child : ch.values()) {
                    findDFSInner(st, idx, child, emitted, arg0, arg1, arg2, arg3, sink);
                }
            } else if ("*".equals(searchPart)) {
                for (Topic<T> child : ch.values()) {
                    findDFSInner(st, idx + 1, child, emitted, arg0, arg1, arg2, arg3, sink);
                }
            } else {
                Topic<T> exact = ch.get(searchPart);
                if (exact != null) findDFSInner(st, idx + 1, exact, emitted, arg0, arg1, arg2, arg3, sink);
                Topic<T> star = node.getStarChild();
                if (star != null) findDFSInner(st, idx + 1, star, emitted, arg0, arg1, arg2, arg3, sink);
                Topic<T> innerDstar = node.getDoubleStarChild();
                if (innerDstar != null) findDFSInner(st, idx, innerDstar, emitted, arg0, arg1, arg2, arg3, sink);
            }
            return;
        }

        Map<String, Topic<T>> children = node.getChildrenMap();
        if (children == null) {
            if ("**".equals(searchPart) && idx == st.length - 1) {
                if (emitted == null || emitted.add(node)) {
                    sink.accept(arg0, arg1, arg2, arg3, node);
                }
            }
            return;
        }

        if ("**".equals(searchPart)) {
            findDFSInner(st, idx + 1, node, emitted, arg0, arg1, arg2, arg3, sink);
            for (Topic<T> child : children.values()) {
                findDFSInner(st, idx, child, emitted, arg0, arg1, arg2, arg3, sink);
            }
        } else if ("*".equals(searchPart)) {
            for (Topic<T> child : children.values()) {
                findDFSInner(st, idx + 1, child, emitted, arg0, arg1, arg2, arg3, sink);
            }
        } else {
            Topic<T> exact = children.get(searchPart);
            if (exact != null) findDFSInner(st, idx + 1, exact, emitted, arg0, arg1, arg2, arg3, sink);
            Topic<T> star = node.getStarChild();
            if (star != null) findDFSInner(st, idx + 1, star, emitted, arg0, arg1, arg2, arg3, sink);
            Topic<T> dstar = node.getDoubleStarChild();
            if (dstar != null) findDFSInner(st, idx, dstar, emitted, arg0, arg1, arg2, arg3, sink);
        }
    }

    private static <T, ARG0, ARG1, ARG2, ARG3> void findDFSInner(
        final SeparatedCharSequence st,
        final int idx,
        final Topic<T> node,
        final Set<Topic<T>> emitted,
        final ARG0 arg0, final ARG1 arg1, final ARG2 arg2, final ARG3 arg3,
        final Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink) {

        final int stSize = st.size();

        if (idx >= stSize) {
            if (emitted == null || emitted.add(node)) {
                sink.accept(arg0, arg1, arg2, arg3, node);
            }
            Topic<T> dstar = node.getDoubleStarChild();
            if (dstar != null) {
                findDFSInner(st, idx, dstar, emitted, arg0, arg1, arg2, arg3, sink);
            }
            return;
        }

        final String searchPart = String.valueOf(st.get(idx));

        if ("**".equals(node.getPart())) {
            findDFSInner(st, idx + 1, node, emitted, arg0, arg1, arg2, arg3, sink);
            Map<String, Topic<T>> ch = node.getChildrenMap();
            if (ch == null) return;
            if ("**".equals(searchPart)) {
                for (Topic<T> child : ch.values()) {
                    findDFSInner(st, idx, child, emitted, arg0, arg1, arg2, arg3, sink);
                }
            } else if ("*".equals(searchPart)) {
                for (Topic<T> child : ch.values()) {
                    findDFSInner(st, idx + 1, child, emitted, arg0, arg1, arg2, arg3, sink);
                }
            } else {
                Topic<T> exact = ch.get(searchPart);
                if (exact != null) findDFSInner(st, idx + 1, exact, emitted, arg0, arg1, arg2, arg3, sink);
                Topic<T> star = node.getStarChild();
                if (star != null) findDFSInner(st, idx + 1, star, emitted, arg0, arg1, arg2, arg3, sink);
                Topic<T> innerDstar = node.getDoubleStarChild();
                if (innerDstar != null) findDFSInner(st, idx, innerDstar, emitted, arg0, arg1, arg2, arg3, sink);
            }
            return;
        }

        Map<String, Topic<T>> children = node.getChildrenMap();
        if (children == null) {
            if ("**".equals(searchPart) && idx == stSize - 1) {
                if (emitted == null || emitted.add(node)) {
                    sink.accept(arg0, arg1, arg2, arg3, node);
                }
            }
            return;
        }

        if ("**".equals(searchPart)) {
            findDFSInner(st, idx + 1, node, emitted, arg0, arg1, arg2, arg3, sink);
            for (Topic<T> child : children.values()) {
                findDFSInner(st, idx, child, emitted, arg0, arg1, arg2, arg3, sink);
            }
        } else if ("*".equals(searchPart)) {
            for (Topic<T> child : children.values()) {
                findDFSInner(st, idx + 1, child, emitted, arg0, arg1, arg2, arg3, sink);
            }
        } else {
            Topic<T> exact = children.get(searchPart);
            if (exact != null) findDFSInner(st, idx + 1, exact, emitted, arg0, arg1, arg2, arg3, sink);
            Topic<T> star = node.getStarChild();
            if (star != null) findDFSInner(st, idx + 1, star, emitted, arg0, arg1, arg2, arg3, sink);
            Topic<T> dstar = node.getDoubleStarChild();
            if (dstar != null) findDFSInner(st, idx, dstar, emitted, arg0, arg1, arg2, arg3, sink);
        }
    }
}
