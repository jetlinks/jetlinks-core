package org.jetlinks.core.message.property;

import org.jetlinks.core.things.ThingProperty;

import javax.annotation.Nullable;

public interface Property extends ThingProperty {
    @Override
    default String getProperty() {
        return getId();
    }
    /**
     * 物模型属性ID
     *
     * @return 属性ID
     */
    String getId();

    /**
     * 时间戳
     *
     * @return 时间戳
     */
    long getTimestamp();

    /**
     * 属性值
     *
     * @return 属性值
     */
    @Nullable
    Object getValue();

    /**
     * 获取属性状态,可能为null
     *
     * @return 属性状态
     */
    @Nullable
    String getState();

}
