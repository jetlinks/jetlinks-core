package org.jetlinks.core.event;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.topic.TopicSubscriptionPlan;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * EventBus 结构化订阅的完整不可变快照。
 * <p>
 * subscriber、features、priority 和 time 确定订阅身份及投递语义；routePlan 表示
 * 可动态更新的 Topic 范围。本地回调只用于当前进程，不参与值语义、编码或集群同步。
 *
 * @see Subscription
 * @see TopicSubscriptionPlan
 * @see EventSubscription
 * @since 1.2.6
 */
@Slf4j
public final class SubscriptionPlan {

    private final String subscriber;
    private final TopicSubscriptionPlan routePlan;
    private final Set<Subscription.Feature> features;
    private final int priority;
    private final long time;
    private final Runnable doOnSubscribe;
    private final Consumer<TopicPayload> onDropped;

    private SubscriptionPlan(Builder builder) {
        this.subscriber = requireSubscriber(builder.subscriber);
        this.routePlan = Objects.requireNonNull(builder.routePlan, "routePlan cannot be null");
        EnumSet<Subscription.Feature> copied = builder.features.isEmpty()
            ? EnumSet.of(Subscription.Feature.local)
            : EnumSet.copyOf(builder.features);
        this.features = Collections.unmodifiableSet(copied);
        this.priority = builder.priority;
        this.time = builder.time;
        this.doOnSubscribe = builder.doOnSubscribe;
        this.onDropped = builder.onDropped;
    }

    /**
     * 创建结构化订阅 builder。
     *
     * @param subscriber 订阅者标识，不能为 {@code null} 或空白
     * @return 新 builder
     * @since 1.2.6
     */
    public static Builder builder(String subscriber) {
        return new Builder(subscriber);
    }

    /**
     * 将旧订阅转换为结构化 Plan，不修改原对象。
     * <p>
     * topics 按旧 {@link org.jetlinks.core.utils.TopicUtils#expand(String)} 规则转换；
     * features、priority、time 及本地 callback 均被保留。
     *
     * @param subscription 旧订阅，不能为 {@code null}
     * @return 完整不可变 Plan
     * @since 1.2.6
     */
    public static SubscriptionPlan from(Subscription subscription) {
        Objects.requireNonNull(subscription, "subscription cannot be null");
        return builder(subscription.getSubscriber())
            .routes(TopicSubscriptionPlan.fromTopics(Arrays.asList(subscription.getTopics())))
            .features(subscription.getFeatures())
            .priority(subscription.getPriority())
            .time(subscription.getTime())
            .doOnSubscribe(subscription.getDoOnSubscribe())
            .onDropped(subscription.getDropListener())
            .build();
    }

    /**
     * @return 订阅者标识
     * @since 1.2.6
     */
    public String getSubscriber() {
        return subscriber;
    }

    /**
     * @return 当前不可变 Route Plan
     * @since 1.2.6
     */
    public TopicSubscriptionPlan getRoutePlan() {
        return routePlan;
    }

    /**
     * @return 不可修改的订阅特性集合
     * @since 1.2.6
     */
    public Set<Subscription.Feature> getFeatures() {
        return features;
    }

    /**
     * @return 订阅优先级
     * @since 1.2.6
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @return 订阅时间
     * @since 1.2.6
     */
    public long getTime() {
        return time;
    }

    /**
     * 只替换 Topic 范围，保留订阅身份、投递特性和本地 callback。
     *
     * @param routePlan 下一完整 Route Plan，不能为 {@code null}
     * @return 新 Plan；Route Plan 未变化时返回当前实例
     * @since 1.2.6
     */
    public SubscriptionPlan withRoutes(TopicSubscriptionPlan routePlan) {
        Objects.requireNonNull(routePlan, "routePlan cannot be null");
        if (this.routePlan.equals(routePlan)) {
            return this;
        }
        return new Builder(this.subscriber)
            .routes(routePlan)
            .features(this.features.toArray(new Subscription.Feature[0]))
            .priority(this.priority)
            .time(this.time)
            .doOnSubscribe(this.doOnSubscribe)
            .onDropped(this.onDropped)
            .build();
    }

    /**
     * 判断另一个 Plan 是否可用于更新当前订阅句柄。
     * <p>
     * 动态更新只允许改变 Route Plan；subscriber、features、priority、time 以及两个
     * 本地 callback 都必须保持不变。callback 使用引用比较，既不暴露 callback，也
     * 避免把不可序列化的本地行为纳入 Plan 值语义。
     *
     * @param nextPlan 待更新的完整 Plan，不能为 {@code null}
     * @return 仅 Route Plan 可能不同时返回 {@code true}
     * @see EventSubscription#updatePlan(SubscriptionPlan)
     * @since 1.2.6
     */
    public boolean isUpdateCompatibleWith(SubscriptionPlan nextPlan) {
        Objects.requireNonNull(nextPlan, "nextPlan cannot be null");
        return subscriber.equals(nextPlan.subscriber)
            && features.equals(nextPlan.features)
            && priority == nextPlan.priority
            && time == nextPlan.time
            && doOnSubscribe == nextPlan.doOnSubscribe
            && onDropped == nextPlan.onDropped;
    }

    /**
     * 通知本地订阅已经成功激活。
     * <p>
     * 具体 EventBus 每次成功激活一个新句柄时调用一次；Plan 更新不重复调用。未配置
     * callback 时为空操作，callback 异常沿当前激活调用链传播。
     *
     * @since 1.2.6
     */
    public void subscribed() {
        if (doOnSubscribe != null) {
            doOnSubscribe.run();
        }
    }

    /**
     * 通知本地 drop listener：消息因当前 Plan 已不匹配而被丢弃。
     *
     * @param payload 被丢弃的消息，不能为 {@code null}
     * @since 1.2.6
     */
    public void discard(TopicPayload payload) {
        notifyDropped(payload);
    }

    /**
     * 通知本地 drop listener：消息因投递缓冲边界而被丢弃。
     *
     * @param payload 被丢弃的消息，不能为 {@code null}
     * @since 1.2.6
     */
    public void dropped(TopicPayload payload) {
        Objects.requireNonNull(payload, "payload cannot be null");
        if (onDropped != null) {
            onDropped.accept(payload);
        } else {
            log.warn(
                "eventbus buffer overflow, drop event:{},subscription:{}",
                payload.getTopic(),
                this
            );
        }
    }

    private void notifyDropped(TopicPayload payload) {
        Objects.requireNonNull(payload, "payload cannot be null");
        if (onDropped != null) {
            onDropped.accept(payload);
        }
    }

    private static String requireSubscriber(String subscriber) {
        if (subscriber == null || subscriber.trim().isEmpty()) {
            throw new IllegalArgumentException("subscriber cannot be empty");
        }
        return subscriber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubscriptionPlan)) {
            return false;
        }
        SubscriptionPlan that = (SubscriptionPlan) o;
        return priority == that.priority
            && time == that.time
            && subscriber.equals(that.subscriber)
            && routePlan.equals(that.routePlan)
            && features.equals(that.features);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriber, routePlan, features, priority, time);
    }

    @Override
    public String toString() {
        return "SubscriptionPlan{" +
            "subscriber='" + subscriber + '\'' +
            ", routePlan=" + routePlan +
            ", features=" + features +
            ", priority=" + priority +
            ", time=" + time +
            '}';
    }

    /**
     * {@link SubscriptionPlan} 构造器。
     * <p>
     * builder 可重复配置，但每次 {@link #build()} 都会创建独立不可变快照。
     *
     * @since 1.2.6
     */
    public static final class Builder {

        private final String subscriber;
        private TopicSubscriptionPlan routePlan = TopicSubscriptionPlan.empty();
        private final EnumSet<Subscription.Feature> features =
            EnumSet.noneOf(Subscription.Feature.class);
        private int priority;
        private long time;
        private Runnable doOnSubscribe;
        private Consumer<TopicPayload> onDropped;

        private Builder(String subscriber) {
            this.subscriber = subscriber;
        }

        /**
         * @param routePlan 完整 Route Plan，不能为 {@code null}
         * @return 当前 builder
         * @since 1.2.6
         */
        public Builder routes(TopicSubscriptionPlan routePlan) {
            this.routePlan = Objects.requireNonNull(routePlan, "routePlan cannot be null");
            return this;
        }

        /**
         * 增加订阅特性；未设置任何特性时默认使用 {@link Subscription.Feature#local}。
         *
         * @param features 订阅特性，不能为 {@code null} 或包含 {@code null}
         * @return 当前 builder
         * @since 1.2.6
         */
        public Builder features(Subscription.Feature... features) {
            if (features == null) {
                throw new IllegalArgumentException("features cannot be null");
            }
            for (Subscription.Feature feature : features) {
                if (feature == null) {
                    throw new IllegalArgumentException("feature cannot be null");
                }
                this.features.add(feature);
            }
            return this;
        }

        /**
         * @param priority 订阅优先级，值越小优先级越高
         * @return 当前 builder
         * @since 1.2.6
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * @param time 订阅时间
         * @return 当前 builder
         * @since 1.2.6
         */
        public Builder time(long time) {
            this.time = time;
            return this;
        }

        /**
         * 配置本地订阅激活回调。回调不参与 Plan 值语义或跨边界编码。
         *
         * @param listener 激活回调，可以为 {@code null}
         * @return 当前 builder
         * @see SubscriptionPlan#subscribed()
         * @since 1.2.6
         */
        public Builder doOnSubscribe(Runnable listener) {
            this.doOnSubscribe = listener;
            return this;
        }

        /**
         * 增加本地消息丢弃回调。多次调用按配置顺序组合，回调不参与值语义或编码。
         *
         * @param listener 丢弃回调，可以为 {@code null}
         * @return 当前 builder
         * @since 1.2.6
         */
        public Builder onDropped(Consumer<TopicPayload> listener) {
            if (listener == null) {
                return this;
            }
            this.onDropped = this.onDropped == null
                ? listener
                : this.onDropped.andThen(listener);
            return this;
        }

        /**
         * @return 独立不可变 Plan
         * @throws IllegalArgumentException subscriber 或特性非法
         * @throws NullPointerException Route Plan 为 {@code null}
         * @since 1.2.6
         */
        public SubscriptionPlan build() {
            return new SubscriptionPlan(this);
        }
    }
}
