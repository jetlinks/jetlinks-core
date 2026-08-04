package org.jetlinks.core.topic;

import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.utils.TopicUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * 对 pattern 中一个命名路径段应用精确允许值集合的不可变 Topic Route。
 * <p>
 * 允许值只负责限制指定路径段；Route 仍会对完整 pattern 做最终匹配。第一版每个
 * Route 只支持一个 indexed segment，且该段之前不能出现 {@code **}。
 *
 * @see TopicRoute
 * @see TopicSubscriptionPlan
 * @since 1.2.6
 */
public final class IndexedTopicRoute implements TopicRoute {

    private final String pattern;
    private final String indexedVariable;
    private final int indexedSegment;
    private final int matchingSegment;
    private final Set<String> allowedValues;
    private final String[] matchingPattern;

    private IndexedTopicRoute(String pattern,
                              String indexedVariable,
                              Collection<? extends CharSequence> allowedValues) {
        this.pattern = PatternTopicRoute.normalize(pattern);
        this.indexedVariable = validateVariable(indexedVariable);
        String[] segments = PatternTopicRoute.parse(this.pattern, true);
        this.matchingSegment = findIndexedSegment(segments, this.indexedVariable);
        this.indexedSegment = matchingSegment + 1;
        this.matchingPattern = segments.clone();
        this.matchingPattern[matchingSegment] = TopicUtils.ANY;
        this.allowedValues = immutableValues(allowedValues);
    }

    /**
     * 创建单集合段 Topic Route。
     *
     * @param pattern 包含一个 {@code {indexedVariable}} 完整路径段的 Topic pattern
     * @param indexedVariable 被精确允许值集合约束的变量名
     * @param allowedValues 允许值集合，可以为空但不能为 {@code null}
     * @return 不可变 Route
     * @throws IllegalArgumentException pattern、变量或允许值不符合契约
     * @since 1.2.6
     */
    public static IndexedTopicRoute of(String pattern,
                                       String indexedVariable,
                                       Collection<? extends CharSequence> allowedValues) {
        return new IndexedTopicRoute(pattern, indexedVariable, allowedValues);
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    /**
     * @return indexed segment 对应的变量名
     * @since 1.2.6
     */
    public String getIndexedVariable() {
        return indexedVariable;
    }

    /**
     * 获取 indexed segment 在标准化路径中的位置。
     * <p>
     * 标准化 pattern 以 {@code /} 开始，因此根路径占索引 {@code 0}。
     *
     * @return indexed segment 索引
     * @since 1.2.6
     */
    public int getIndexedSegment() {
        return indexedSegment;
    }

    /**
     * @return 排序、去重后的不可修改允许值集合
     * @since 1.2.6
     */
    public Set<String> getAllowedValues() {
        return allowedValues;
    }

    /**
     * 使用新的完整允许值集合创建 Route。
     *
     * @param allowedValues 新的完整允许值集合，可以为空但不能为 {@code null}
     * @return 新 Route；值未变化时返回当前实例
     * @since 1.2.6
     */
    public IndexedTopicRoute withAllowedValues(
        Collection<? extends CharSequence> allowedValues) {
        Set<String> next = immutableValues(allowedValues);
        if (this.allowedValues.equals(next)) {
            return this;
        }
        return new IndexedTopicRoute(pattern, indexedVariable, next);
    }

    @Override
    public boolean matches(SeparatedCharSequence topic) {
        Objects.requireNonNull(topic, "topic cannot be null");
        if (allowedValues.isEmpty() || topic.separator() != TopicUtils.PATH_SPLITTER) {
            return false;
        }
        CharSequence first = topic.size() > 0 ? topic.get(0) : null;
        int topicOffset = topic.size() > 0 && first != null && first.length() == 0 ? 1 : 0;
        int valueIndex = topicOffset + matchingSegment;
        if (valueIndex >= topic.size()) {
            return false;
        }
        CharSequence segment = topic.get(valueIndex);
        if (segment == null || !allowedValues.contains(segment instanceof String ? (String) segment : segment.toString())) {
            return false;
        }
        return PatternTopicRoute.matches(matchingPattern, topic);
    }

    private static String validateVariable(String variable) {
        if (variable == null || variable.trim().isEmpty()) {
            throw new IllegalArgumentException("indexed variable cannot be empty");
        }
        if (variable.indexOf('/') >= 0
            || variable.indexOf('*') >= 0
            || variable.indexOf('{') >= 0
            || variable.indexOf('}') >= 0) {
            throw new IllegalArgumentException("invalid indexed variable: " + variable);
        }
        return variable;
    }

    private static int findIndexedSegment(String[] segments, String variable) {
        String expected = '{' + variable + '}';
        int indexed = -1;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (PatternTopicRoute.isVariable(segment)) {
                if (!expected.equals(segment) || indexed >= 0) {
                    throw new IllegalArgumentException(
                        "pattern must contain exactly one indexed variable: " + expected
                    );
                }
                indexed = i;
            }
        }
        if (indexed < 0) {
            throw new IllegalArgumentException("indexed variable not found: " + expected);
        }
        for (int i = 0; i < indexed; i++) {
            if (TopicUtils.ANY_ALL.equals(segments[i])) {
                throw new IllegalArgumentException(
                    "** cannot appear before the indexed segment"
                );
            }
        }
        return indexed;
    }

    private static Set<String> immutableValues(
        Collection<? extends CharSequence> allowedValues) {
        if (allowedValues == null) {
            throw new IllegalArgumentException("allowed values cannot be null");
        }
        TreeSet<String> values = new TreeSet<>();
        for (CharSequence value : allowedValues) {
            if (value == null) {
                throw new IllegalArgumentException("allowed value cannot be null");
            }
            String string = value.toString();
            if (string.isEmpty()
                || string.indexOf('/') >= 0
                || string.indexOf('*') >= 0
                || string.indexOf('{') >= 0
                || string.indexOf('}') >= 0) {
                throw new IllegalArgumentException("invalid allowed value: " + string);
            }
            values.add(string);
        }
        return Collections.unmodifiableSet(values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndexedTopicRoute)) {
            return false;
        }
        IndexedTopicRoute that = (IndexedTopicRoute) o;
        return pattern.equals(that.pattern)
            && indexedVariable.equals(that.indexedVariable)
            && allowedValues.equals(that.allowedValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, indexedVariable, allowedValues);
    }

    @Override
    public String toString() {
        return "IndexedTopicRoute{" +
            "pattern='" + pattern + '\'' +
            ", indexedVariable='" + indexedVariable + '\'' +
            ", allowedValues=" + allowedValues +
            '}';
    }
}
