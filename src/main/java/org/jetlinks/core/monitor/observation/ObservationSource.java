package org.jetlinks.core.monitor.observation;

import reactor.util.context.ContextView;

import java.util.Objects;
import java.util.Optional;

/**
 * 由可信业务入口声明的单次业务来源，不是资产权限或实际输入集合。
 * 框架可将有界的类型和标识传递到消息、远程调用及持久任务，不能传递业务对象。
 * 来源不依赖采样；无记录器时仍可由业务入口绑定以保存必要的来源事实。
 *
 * @param type 来源类型，由声明方定义
 * @param id 单次发生标识，不能用配置或设备标识冒充
 * @since 1.3.3
 * @see ObservationReference
 */
public record ObservationSource(String type, String id) {

    public ObservationSource {
        validate(type, 64, "type");
        validate(id, 128, "id");
    }

    private static void validate(String value, int limit, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank() || value.length() > limit
            || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("invalid observation source " + name);
        }
    }

    /**
     * 读取订阅上下文中的显式来源；不从业务 payload、普通 id 或归因主体推断。
     *
     * @param context 当前订阅上下文
     * @return 已声明的来源，无绑定时为空
     * @since 1.3.3
     */
    public static Optional<ObservationSource> current(ContextView context) {
        Object value = context.getOrDefault(ObservationSource.class, null);
        return value instanceof ObservationSource source ? Optional.of(source) : Optional.empty();
    }
}
