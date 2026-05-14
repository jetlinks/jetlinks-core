package org.jetlinks.core.monitor.recorder;

import org.jetlinks.core.Key;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 操作记录器,用于记录某个操作.
 *
 * @param <E> 操作产生的数据类型.
 * @author zhouhao
 * @since 1.3.1
 */
public interface ActionRecorder<E> extends Function<Publisher<E>, Publisher<E>> {

    @SuppressWarnings("unchecked")
    static <E> ActionRecorder<E> noop() {
        return (ActionRecorder<E>) NoopActionRecorder.INSTANCE;
    }

    /**
     * 设置标签
     *
     * @param tag   tag
     * @param value value
     * @return ActionRecorder
     */
    ActionRecorder<E> tag(String tag, Object value);

    /**
     * 设置标签
     *
     * @param key   key
     * @param value value
     * @param <V>   值类型
     * @return this
     */
    <V> ActionRecorder<E> tag(Key<V> key, V value);

    /**
     * 设置标签
     *
     * @param key   key
     * @param value value
     * @param <V>   值类型
     * @return this
     */
    <V> ActionRecorder<E> tag(Key<V> key, Supplier<V> value);

    /**
     * 设置多个标签
     *
     * @param tags tags
     * @return ActionRecorder
     */
    ActionRecorder<E> tags(Map<String, Object> tags);

    /**
     * 记录属性数据,属性重要性低于标签,可能不会被存储.
     *
     * @param key   数据标识
     * @param value 数据值
     * @return ActionRecorder
     */
    ActionRecorder<E> attribute(String key, Object value);

    /**
     * 设置属性
     *
     * @param key   key
     * @param value value
     * @param <V>   值类型
     * @return this
     */
    <V> ActionRecorder<E> attribute(Key<V> key, V value);

    /**
     * 设置属性
     *
     * @param key   key
     * @param value value
     * @param <V>   值类型
     * @return this
     */
    <V> ActionRecorder<E> attribute(Key<V> key, Supplier<V> value);

    /**
     * 记录多个属性数据,属性重要性低于标签,可能不会被存储.
     *
     * @param data 数据键值对
     * @return ActionRecorder
     */
    ActionRecorder<E> attributes(Map<String, Object> data);

    /**
     * 记录发生错误
     *
     * @param error 错误信息
     * @return ActionRecorder
     */
    ActionRecorder<E> error(Throwable error);

    /**
     * 记录操作取消
     *
     * @return ActionRecorder
     */
    ActionRecorder<E> cancel();

    /**
     * 记录操作完成
     *
     * @return ActionRecorder
     */
    ActionRecorder<E> complete();

    /**
     * 记录操作产生的结果值
     *
     * @param value 结果值
     * @return value
     */
    ActionRecorder<E> value(E value);

    /**
     * 定义值转换逻辑.
     *
     * @param converter converter
     * @return converter
     */
    ActionRecorder<E> valueConverter(Function<E, Object> converter);

    /**
     * 开始记录
     *
     * @param context 上下文
     * @return ActionRecorder
     */
    ActionRecorder<E> start(ContextView context);

    /**
     * 直接记录已有的操作结果.
     * <p>
     * 默认实现会尽可能通过公开 API 回放记录内容,如果实现类支持更完整的结果透传,
     * 可以覆盖此方法来保留原始记录中的更多上下文信息.
     *
     * @param record 已有的操作记录
     * @return this
     */
    default ActionRecorder<E> record(ActionRecord record) {
        if (record == null) {
            return this;
        }
        if (record.getTags() != null) {
            tags(record.getTags());
        }
        if (record.getAttributes() != null) {
            attributes(record.getAttributes());
        }
        if (record.isCancel()) {
            return cancel();
        }
        if (record.isHasError()) {
            return error(new RuntimeException(record.getErrorDetail()));
        }
        return complete();
    }

    /**
     * 创建一个子操作
     *
     * @param action 子操作
     * @return ActionRecorder
     */
    <T> ActionRecorder<T> child(CharSequence action);

    /**
     * 转换包装Publisher
     *
     * @param publisher publisher
     * @return publisher
     * @see Mono#transform(Function)
     * @see Flux#transform(Function)
     */
    @Override
    default Publisher<E> apply(Publisher<E> publisher) {
        if (publisher instanceof Mono<?>) {
            return new MetricsActionMono<>(this, ((Mono<E>) publisher));
        }
        if (publisher instanceof Flux<?>) {
            return new MetricsActionFlux<>(this, ((Flux<E>) publisher));
        }
        return publisher;
    }


}
