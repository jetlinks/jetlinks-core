package org.jetlinks.core.monitor.observation;

import reactor.util.context.ContextView;

/**
 * 按一次实际订阅选择附加记录器及详细元数据采集。
 *
 * 实现只读取可信上下文与本地生效快照，不阻塞或发起远程查询。返回值不授予业务权限，
 * 也不替代原 Monitor 的记录器。相同来源执行的策略快照由入口负责固定。
 *
 * @since 1.3.3
 * @see RecorderSelection
 */
@FunctionalInterface
public interface RecorderProvider {

    /**
     * 在业务操作订阅时选择一次；多个订阅可并发调用，实现不得阻塞或修改业务上下文。
     *
     * @param action  稳定操作名
     * @param context 本次订阅上下文，不得跨执行长期持有
     * @return 非空、不可变的本次选择；调用异常仅使详细采集失败，不改变业务信号
     * @since 1.3.3
     * @see RecorderSelection
     */
    RecorderSelection select(String action, ContextView context);

    /**
     * @return 保留原记录器但不采集详细元数据的固定选择器
     */
    static RecorderProvider disabled() {
        return Disabled.INSTANCE;
    }

    /**
     * 无可变状态的禁用实现，通过 {@link RecorderProvider#disabled()} 获取。
     */
    enum Disabled implements RecorderProvider {
        INSTANCE;

        @Override
        public RecorderSelection select(String action, ContextView context) {
            return RecorderSelection.disabled();
        }
    }
}
