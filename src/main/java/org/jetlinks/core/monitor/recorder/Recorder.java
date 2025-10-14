package org.jetlinks.core.monitor.recorder;

import jakarta.annotation.Nonnull;

/**
 * 监控记录器
 *
 * @author zhouhao
 * @since 1.3.1
 */
public interface Recorder {

    /**
     * 获取什么也不做的记录器
     * @return Recorder
     */
    static Recorder noop() {
        return NoopRecorder.INSTANCE;
    }

    /**
     * 获取一个指定标识的记录器
     * @return ActionRecorder
     * @param <E> E
     */
    <E> ActionRecorder<E> action(@Nonnull CharSequence action);

}
