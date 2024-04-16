package org.jetlinks.core.things;

/**
 * 标签.
 *
 * @author zhangji 2024/4/15
 * @since 1.2.2
 */
public interface ThingTag {

    /**
     * @return 标签ID
     */
    String getTag();

    /**
     * @return 标签值
     */
    Object getValue();

    /**
     * @return 时间戳
     */
    long getTimestamp();

}
