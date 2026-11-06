package org.jetlinks.core.monitor.observation;

import reactor.util.context.ContextView;

import java.util.Objects;
import java.util.Optional;

/**
 * 一次实际执行的轻量引用，可由原消息或任务保存并在接收端恢复。
 * 引用不包含活 action、记录器或权限；原生记录通过保留标签关联，不能将其解释为提交成功。
 * 跨节点恢复后新建本地执行，把此引用作为上游，不复用上游生命周期。
 *
 * @param executionId 有界、不含控制字符的执行标识
 * @since 1.3.3
 * @see ObservationKeys
 */
public record ObservationReference(String executionId) {

    public ObservationReference {
        Objects.requireNonNull(executionId, "executionId");
        if (executionId.isBlank() || executionId.length() > 128
            || executionId.chars().anyMatch(character -> Character.isISOControl(character))) {
            throw new IllegalArgumentException("invalid observation execution reference");
        }
    }

    /**
     * 读取本次订阅已有的引用；没有观测或引用时为空，不查找全局状态。
     *
     * @param context 本地订阅上下文
     * @return 当前执行引用，不包含 Recorder 或身份信息
     * @since 1.3.3
     */
    public static Optional<ObservationReference> current(ContextView context) {
        Object value = context.getOrDefault(ObservationReference.class, null);
        return value instanceof ObservationReference reference ? Optional.of(reference) : Optional.empty();
    }
}
