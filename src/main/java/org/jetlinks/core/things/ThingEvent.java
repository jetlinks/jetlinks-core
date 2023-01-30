package org.jetlinks.core.things;

public interface ThingEvent {

    /**
     * @return 物模型事件ID
     */
    String getEvent();

    /**
     * @return 时间戳
     */
    long getTimestamp();

    /**
     * @return 事件数据
     */
    Object getData();
}
