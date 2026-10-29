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
     * 组合接收端并隔离非 fatal 回调异常；保留原生 action 类型与父子关系。
     * 去除已知 noop 与重复实例。应用组合 action 只订阅业务一次，不对普通记录进行通用回放。
     *
     * @param recorders 原记录器，不能包含 null
     * @return 每次 action 调用创建独立生命周期的安全组合
     * @since 1.3.3
     */
    static Recorder combine(Recorder... recorders) {
        return CompositeRecorder.create(recorders);
    }

    /**
     * 为本次选中的接收端继承组合父操作。只有原接收端实例匹配时才调用其原生 child，
     * 新接收端创建根操作；不会重新加入本次未选择的接收端或修改原生记录 ID。
     *
     * @param recorder 本次选中的接收端
     * @param parent   由 combine 创建的本地父操作；普通 action 没有接收端身份映射时不猜测归属
     * @return 按调用创建未启动子操作的记录器
     * @since 1.3.3
     * @see #combine(Recorder...)
     */
    static Recorder withParent(Recorder recorder, ActionRecorder<?> parent) {
        return CompositeRecorder.withParent(recorder, parent);
    }

    /**
     * 直接记录已有的操作结果.
     *
     * @param record 已有的操作记录
     */
    default void record(ActionRecord record) {
        if (record == null || record.getAction() == null) {
            return;
        }
        ActionRecordReplayer.replay(action(record.getAction()), record);
    }

    /**
     * 获取一个指定标识的记录器
     * @return ActionRecorder
     * @param <E> E
     */
    <E> ActionRecorder<E> action(@Nonnull CharSequence action);

}
