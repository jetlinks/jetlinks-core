package org.jetlinks.core.topic;

import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.utils.TopicUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * 使用现有 exact、{@code *}、{@code **} 语义匹配 Topic 的不可变 Route。
 * <p>
 * pattern 在构造时完成标准化和分段，匹配热路径不再解析 pattern。
 *
 * @see TopicFinder
 * @see TopicUtils
 * @since 1.2.6
 */
public final class PatternTopicRoute implements TopicRoute {

    private final String pattern;
    private final String[] segments;

    private PatternTopicRoute(String pattern) {
        this.pattern = normalize(pattern);
        this.segments = parse(this.pattern, false);
    }

    /**
     * 创建 Topic pattern Route。
     *
     * @param pattern exact、{@code *}、{@code **} 组成的 Topic pattern
     * @return 不可变 Route
     * @throws IllegalArgumentException pattern 为空、包含空路径段、内嵌通配符或变量段
     * @since 1.2.6
     */
    public static PatternTopicRoute of(String pattern) {
        return new PatternTopicRoute(pattern);
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public boolean matches(SeparatedCharSequence topic) {
        return matches(segments, topic);
    }

    static String normalize(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("pattern cannot be empty");
        }
        String normalized = pattern.charAt(0) == '/' ? pattern : '/' + pattern;
        if (normalized.length() > 1
            && normalized.charAt(normalized.length() - 1) == '/') {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    static String[] parse(String pattern, boolean allowVariables) {
        String[] raw = TopicUtils.split(pattern, false, false);
        int start = raw.length > 0 && raw[0].isEmpty() ? 1 : 0;
        String[] segments = Arrays.copyOfRange(raw, start, raw.length);
        if (segments.length == 1 && segments[0].isEmpty() && "/".equals(pattern)) {
            return new String[0];
        }
        for (String segment : segments) {
            validateSegment(segment, allowVariables);
        }
        return segments;
    }

    private static void validateSegment(String segment, boolean allowVariables) {
        if (segment.isEmpty()) {
            throw new IllegalArgumentException("pattern cannot contain empty segment");
        }
        if (TopicUtils.ANY.equals(segment) || TopicUtils.ANY_ALL.equals(segment)) {
            return;
        }
        if (segment.indexOf('*') >= 0) {
            throw new IllegalArgumentException("wildcard must occupy a complete segment");
        }
        boolean containsBrace = segment.indexOf('{') >= 0 || segment.indexOf('}') >= 0;
        if (containsBrace && (!allowVariables || !isVariable(segment))) {
            throw new IllegalArgumentException("invalid variable segment: " + segment);
        }
    }

    static boolean isVariable(String segment) {
        return segment.length() > 2
            && segment.charAt(0) == '{'
            && segment.charAt(segment.length() - 1) == '}'
            && segment.indexOf('{', 1) < 0
            && segment.indexOf('}') == segment.length() - 1;
    }

    static boolean matches(String[] pattern, SeparatedCharSequence topic) {
        Objects.requireNonNull(topic, "topic cannot be null");
        if (topic.separator() != TopicUtils.PATH_SPLITTER) {
            return false;
        }
        int offset = topic.size() > 0 && isEmpty(topic.get(0)) ? 1 : 0;
        if (topic.size() == offset) {
            return pattern.length == 0
                || (topic.size() > 0
                    && pattern.length == 1
                    && TopicUtils.ANY_ALL.equals(pattern[0]));
        }
        return matches(pattern, 0, topic, offset);
    }

    private static boolean matches(String[] pattern,
                                   int patternIndex,
                                   SeparatedCharSequence topic,
                                   int topicIndex) {
        if (patternIndex == pattern.length) {
            return topicIndex == topic.size();
        }
        String segment = pattern[patternIndex];
        if (TopicUtils.ANY_ALL.equals(segment)) {
            if (patternIndex == pattern.length - 1) {
                return true;
            }
            for (int index = topicIndex; index <= topic.size(); index++) {
                if (matches(pattern, patternIndex + 1, topic, index)) {
                    return true;
                }
            }
            return false;
        }
        if (topicIndex >= topic.size()) {
            return false;
        }
        if (!TopicUtils.ANY.equals(segment) && !contentEquals(segment, topic.get(topicIndex))) {
            return false;
        }
        return matches(pattern, patternIndex + 1, topic, topicIndex + 1);
    }

    private static boolean isEmpty(CharSequence value) {
        return value == null || value.length() == 0;
    }

    private static boolean contentEquals(String expected, CharSequence actual) {
        if (actual == null || expected.length() != actual.length()) {
            return false;
        }
        for (int i = 0; i < expected.length(); i++) {
            if (expected.charAt(i) != actual.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PatternTopicRoute)) {
            return false;
        }
        PatternTopicRoute that = (PatternTopicRoute) o;
        return pattern.equals(that.pattern);
    }

    @Override
    public int hashCode() {
        return pattern.hashCode();
    }

    @Override
    public String toString() {
        return "PatternTopicRoute{" +
            "pattern='" + pattern + '\'' +
            '}';
    }
}
