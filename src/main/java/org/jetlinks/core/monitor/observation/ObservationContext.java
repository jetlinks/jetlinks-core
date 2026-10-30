package org.jetlinks.core.monitor.observation;

import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 观测引用与框架消息头之间的白名单适配，不编码活记录器或身份权限。
 * 业务 payload 不参与来源推断；同一持续流必须逐消息恢复上下文。
 *
 * @since 1.3.3
 * @see ObservationReference
 * @see ObservationSource
 */
public final class ObservationContext {

    private ObservationContext() {
    }

    /** 将当前已有的有界引用写入框架载体，无绑定时不创建引用。 */
    public static void writeTo(ContextView context, BiConsumer<String, Object> writer) {
        ObservationReference.current(context).ifPresent(reference ->
            writer.accept(ObservationKeys.EXECUTION_ID.getKey(), reference.executionId()));
        ObservationSource.current(context).ifPresent(source -> {
            writer.accept(ObservationKeys.SOURCE_TYPE.getKey(), source.type());
            writer.accept(ObservationKeys.SOURCE_ID.getKey(), source.id());
        });
    }

    /**
     * 从框架载体恢复轻量引用。无效观测字段独立丢弃，不影响业务与原认证上下文。
     * 此方法不验证业务来源的访问权限，不能用于授权。
     */
    public static Context readToContext(Context context, Map<?, ?> headers) {
        if (headers == null || headers.isEmpty()) {
            return context;
        }
        if (headers.containsKey(ObservationKeys.EXECUTION_ID.getKey())) {
            context = context.delete(ObservationReference.class);
        }
        if (headers.get(ObservationKeys.EXECUTION_ID.getKey()) instanceof String) {
            String id = (String) headers.get(ObservationKeys.EXECUTION_ID.getKey());
            try {
                context = context.put(ObservationReference.class, new ObservationReference(id));
            } catch (IllegalArgumentException ignored) {
                // 观测头不参与业务控制；损坏的引用不能冒充有效上游。
                context = context.delete(ObservationReference.class);
            }
        }
        if (headers.containsKey(ObservationKeys.SOURCE_TYPE.getKey()) || headers.containsKey(ObservationKeys.SOURCE_ID.getKey())) {
            context = context.delete(ObservationSource.class);
        }
        if (headers.get(ObservationKeys.SOURCE_TYPE.getKey()) instanceof String
            && headers.get(ObservationKeys.SOURCE_ID.getKey()) instanceof String) {
            String type = (String) headers.get(ObservationKeys.SOURCE_TYPE.getKey());
            String id = (String) headers.get(ObservationKeys.SOURCE_ID.getKey());
            try {
                context = context.put(ObservationSource.class, new ObservationSource(type, id));
            } catch (IllegalArgumentException ignored) {
                context = context.delete(ObservationSource.class);
            }
        }
        return context;
    }
}
