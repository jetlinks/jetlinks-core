package org.jetlinks.core.topic;

/**
 * Route Plan 解码时由调用方显式提供的不可变资源上限。
 * <p>
 * core 不提供猜测性的业务默认值；上层必须结合 frame、内存限制和真实数据规模创建。
 *
 * @see TopicSubscriptionPlanCodec#decode(java.io.DataInput, int, TopicSubscriptionPlanDecodeLimits)
 * @since 1.2.6
 */
public final class TopicSubscriptionPlanDecodeLimits {

    private final int maxRoutes;
    private final int maxIndexedValues;
    private final int maxValueLength;
    private final int maxPayloadLength;

    /**
     * @param maxRoutes 完整 Plan 允许的最大 Route 数，必须大于 {@code 0}
     * @param maxIndexedValues 完整 Plan 允许的最大 indexed value 总数，必须大于 {@code 0}
     * @param maxValueLength 单个字符串值允许的最大长度，必须大于 {@code 0}
     * @param maxPayloadLength payload 允许的最大字节长度，必须大于 {@code 0}
     * @throws IllegalArgumentException 任一上限小于等于 {@code 0}
     * @since 1.2.6
     */
    public TopicSubscriptionPlanDecodeLimits(int maxRoutes,
                                             int maxIndexedValues,
                                             int maxValueLength,
                                             int maxPayloadLength) {
        this.maxRoutes = positive(maxRoutes, "maxRoutes");
        this.maxIndexedValues = positive(maxIndexedValues, "maxIndexedValues");
        this.maxValueLength = positive(maxValueLength, "maxValueLength");
        this.maxPayloadLength = positive(maxPayloadLength, "maxPayloadLength");
    }

    /**
     * @return 完整 Plan 允许的最大 Route 数
     * @since 1.2.6
     */
    public int getMaxRoutes() {
        return maxRoutes;
    }

    /**
     * @return 完整 Plan 允许的最大 indexed value 总数
     * @since 1.2.6
     */
    public int getMaxIndexedValues() {
        return maxIndexedValues;
    }

    /**
     * @return 单个字符串值允许的最大长度
     * @since 1.2.6
     */
    public int getMaxValueLength() {
        return maxValueLength;
    }

    /**
     * @return payload 允许的最大字节长度
     * @since 1.2.6
     */
    public int getMaxPayloadLength() {
        return maxPayloadLength;
    }

    private static int positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
        return value;
    }
}
