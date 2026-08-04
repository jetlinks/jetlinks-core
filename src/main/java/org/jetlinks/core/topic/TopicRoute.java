package org.jetlinks.core.topic;

import org.jetlinks.core.lang.SeparatedCharSequence;

/**
 * 一个不可变的 Topic 匹配规则。
 * <p>
 * Route 负责最终精确判定；索引只能使用其结构定位候选，不能替代
 * {@link #matches(SeparatedCharSequence)}。实现必须线程安全，且不得在匹配时执行
 * 阻塞操作。
 *
 * @see PatternTopicRoute
 * @see IndexedTopicRoute
 * @since 1.2.6
 */
public interface TopicRoute {

    /**
     * 获取标准化后的 Topic pattern。
     *
     * @return Topic pattern，不会返回 {@code null}
     * @since 1.2.6
     */
    String getPattern();

    /**
     * 对完整 Topic 执行最终精确判定。
     *
     * @param topic 已按 {@code /} 分段的完整 Topic，不能为 {@code null}
     * @return 匹配返回 {@code true}
     * @since 1.2.6
     */
    boolean matches(SeparatedCharSequence topic);
}
