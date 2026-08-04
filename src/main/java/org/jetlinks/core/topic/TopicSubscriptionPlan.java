package org.jetlinks.core.topic;

import org.jetlinks.core.event.SubscriptionPlan;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.utils.TopicUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 多个 {@link TopicRoute} 的不可变 OR 组合。
 * <p>
 * 构造时对相同 Route 去重并形成确定性顺序；一个 Topic 同时命中多个 Route 时，
 * {@link #matches(SeparatedCharSequence)} 仍只返回一个布尔结果。
 *
 * @see TopicRoute
 * @see SubscriptionPlan
 * @since 1.2.6
 */
public final class TopicSubscriptionPlan {

    private static final TopicSubscriptionPlan EMPTY =
        new TopicSubscriptionPlan(Collections.emptyList());

    private static final Comparator<TopicRoute> ROUTE_COMPARATOR =
        Comparator.comparingInt(TopicSubscriptionPlan::routeType)
            .thenComparing(TopicRoute::getPattern)
            .thenComparing(TopicSubscriptionPlan::routeDetail);

    private final List<TopicRoute> routes;

    private TopicSubscriptionPlan(Collection<? extends TopicRoute> routes) {
        LinkedHashSet<TopicRoute> unique = new LinkedHashSet<>();
        for (TopicRoute route : routes) {
            if (route == null) {
                throw new IllegalArgumentException("route cannot be null");
            }
            if (!(route instanceof PatternTopicRoute) && !(route instanceof IndexedTopicRoute)) {
                throw new IllegalArgumentException("unsupported route type: " + route.getClass().getName());
            }
            unique.add(route);
        }
        List<TopicRoute> ordered = new ArrayList<>(unique);
        ordered.sort(ROUTE_COMPARATOR);
        this.routes = Collections.unmodifiableList(ordered);
    }

    /**
     * @return 不匹配任何 Topic 的共享空 Plan
     * @since 1.2.6
     */
    public static TopicSubscriptionPlan empty() {
        return EMPTY;
    }

    /**
     * 使用 Route 创建完整 Plan。
     *
     * @param routes Route 数组，不能为 {@code null} 或包含 {@code null}
     * @return 不可变 Plan
     * @since 1.2.6
     */
    public static TopicSubscriptionPlan of(TopicRoute... routes) {
        if (routes == null) {
            throw new IllegalArgumentException("routes cannot be null");
        }
        return of(Arrays.asList(routes));
    }

    /**
     * 使用 Route 集合创建完整 Plan。
     *
     * @param routes Route 集合，不能为 {@code null} 或包含 {@code null}
     * @return 不可变 Plan
     * @since 1.2.6
     */
    public static TopicSubscriptionPlan of(Collection<? extends TopicRoute> routes) {
        if (routes == null) {
            throw new IllegalArgumentException("routes cannot be null");
        }
        return routes.isEmpty() ? EMPTY : new TopicSubscriptionPlan(routes);
    }

    /**
     * 将旧 Topic 集合按 {@link TopicUtils#expand(String)} 规则转换为 pattern Route。
     * <p>
     * 该方法不会把变量段猜测成 indexed route；需要集合段索引时必须显式创建
     * {@link IndexedTopicRoute}。
     *
     * @param topics 旧 Topic 集合，不能为 {@code null} 或包含 {@code null}
     * @return 不可变 Plan
     * @since 1.2.6
     */
    public static TopicSubscriptionPlan fromTopics(
        Collection<? extends CharSequence> topics) {
        if (topics == null) {
            throw new IllegalArgumentException("topics cannot be null");
        }
        List<TopicRoute> routes = new ArrayList<>();
        for (CharSequence topic : topics) {
            if (topic == null) {
                throw new IllegalArgumentException("topic cannot be null");
            }
            for (String expanded : TopicUtils.expand(topic.toString())) {
                routes.add(PatternTopicRoute.of(expanded));
            }
        }
        return of(routes);
    }

    /**
     * @return 确定性排序后的不可修改 Route 列表
     * @since 1.2.6
     */
    public List<TopicRoute> getRoutes() {
        return routes;
    }

    /**
     * @return 不包含 Route 时返回 {@code true}
     * @since 1.2.6
     */
    public boolean isEmpty() {
        return routes.isEmpty();
    }

    /**
     * 按 OR 语义匹配 Topic。
     *
     * @param topic 完整已分段 Topic，不能为 {@code null}
     * @return 任意 Route 匹配时返回 {@code true}
     * @since 1.2.6
     */
    public boolean matches(SeparatedCharSequence topic) {
        Objects.requireNonNull(topic, "topic cannot be null");
        for (TopicRoute route : routes) {
            if (route.matches(topic)) {
                return true;
            }
        }
        return false;
    }

    private static int routeType(TopicRoute route) {
        if (route instanceof PatternTopicRoute) {
            return 0;
        }
        if (route instanceof IndexedTopicRoute) {
            return 1;
        }
        return 2;
    }

    private static String routeDetail(TopicRoute route) {
        if (route instanceof IndexedTopicRoute) {
            IndexedTopicRoute indexed = (IndexedTopicRoute) route;
            return indexed.getIndexedVariable() + '\u0000' + indexed.getAllowedValues();
        }
        if (route instanceof PatternTopicRoute) {
            return "";
        }
        return route.getClass().getName() + '\u0000' + route;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TopicSubscriptionPlan)) {
            return false;
        }
        TopicSubscriptionPlan that = (TopicSubscriptionPlan) o;
        return routes.equals(that.routes);
    }

    @Override
    public int hashCode() {
        return routes.hashCode();
    }

    @Override
    public String toString() {
        return "TopicSubscriptionPlan{" +
            "routes=" + routes +
            '}';
    }
}
