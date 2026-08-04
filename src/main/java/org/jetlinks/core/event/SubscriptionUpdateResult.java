package org.jetlinks.core.event;

import java.util.Objects;

/**
 * 一次完整订阅 Plan 更新的不可变结果。
 * <p>
 * revision 只在当前句柄生命周期内单调；相同 Plan 的幂等更新返回当前 revision，且
 * {@link #isChanged()} 为 {@code false}。
 *
 * @see EventSubscription#updatePlan(SubscriptionPlan)
 * @see SubscriptionSynchronization
 * @since 1.2.6
 */
public final class SubscriptionUpdateResult {

    private final long revision;
    private final boolean changed;
    private final SubscriptionSynchronization synchronization;

    /**
     * @param revision 当前句柄 revision，不能为负数
     * @param changed 本次调用是否实际改变 Plan
     * @param synchronization 本地生效后的同步状态，不能为 {@code null}
     * @since 1.2.6
     */
    public SubscriptionUpdateResult(long revision,
                                    boolean changed,
                                    SubscriptionSynchronization synchronization) {
        if (revision < 0) {
            throw new IllegalArgumentException("revision cannot be negative");
        }
        this.revision = revision;
        this.changed = changed;
        this.synchronization = Objects.requireNonNull(
            synchronization,
            "synchronization cannot be null"
        );
    }

    /**
     * @return 当前句柄 revision
     * @since 1.2.6
     */
    public long getRevision() {
        return revision;
    }

    /**
     * @return 本次调用实际改变 Plan 时返回 {@code true}
     * @since 1.2.6
     */
    public boolean isChanged() {
        return changed;
    }

    /**
     * @return 本地 Plan 生效后的同步状态
     * @since 1.2.6
     */
    public SubscriptionSynchronization getSynchronization() {
        return synchronization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubscriptionUpdateResult)) {
            return false;
        }
        SubscriptionUpdateResult that = (SubscriptionUpdateResult) o;
        return revision == that.revision
            && changed == that.changed
            && synchronization == that.synchronization;
    }

    @Override
    public int hashCode() {
        return Objects.hash(revision, changed, synchronization);
    }

    @Override
    public String toString() {
        return "SubscriptionUpdateResult{" +
            "revision=" + revision +
            ", changed=" + changed +
            ", synchronization=" + synchronization +
            '}';
    }
}
