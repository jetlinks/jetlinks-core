package org.jetlinks.core.message.property;

import javax.annotation.Nullable;

public interface Property {

    /**
     * 物模型属性ID
     * @return 属性ID
     */
    String getId();

    /**
     * 时间戳
     * @return 时间戳
     */
    long getTimestamp();

    /**
     * 属性值
     * @return 属性值
     */
    @Nullable
    Object getValue();

    /**
     * 获取属性状态,可能为null
     * @return 属性状态
     */
    @Nullable
    String getState();

}
