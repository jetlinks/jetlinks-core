package org.jetlinks.core.things;

import javax.annotation.Nonnull;

/**
 * 物属性，表示一个物实例的属性值
 *
 * @author zhouhao
 * @since 1.12
 */
public interface ThingProperty {

    /**
     * @return 属性ID
     */
    String getProperty();

    /**
     * @return 属性值
     */
    Object getValue();

    /**
     * @return 时间戳
     */
    long getTimestamp();

    /**
     * @return 状态
     */
    String getState();

    static ThingProperty of(@Nonnull String property, @Nonnull Object value, long timestamp) {
        return of(property, value, timestamp, null);
    }

    static ThingProperty of(@Nonnull String property, @Nonnull Object value, long timestamp, String state) {
        return new ReadOnlyThingProperty(property, value, timestamp, state);
    }
}
