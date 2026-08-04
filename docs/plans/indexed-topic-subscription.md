# jetlinks-core 可动态更新的紧凑 Topic 订阅 SPI 设计

状态：SPI 设计与本期 core 契约范围已确认；开发任务已制定，待开始实现。

公共 API 版本：`@since 1.2.6`。

关联 Issue：

- [#90 feat(eventbus): 支持可动态更新的订阅句柄](https://github.com/jetlinks/jetlinks-core/issues/90)
- [#91 perf(topic): 支持集合段索引的紧凑 Topic 订阅](https://github.com/jetlinks/jetlinks-core/issues/91)

## 1. 背景与当前结论

当前 `EventBus` 仅接受一次性的 `Subscription`：

```java
Flux<TopicPayload> subscribe(Subscription subscription);

Cancelable subscribe(
    Subscription subscription,
    Function<TopicPayload, Mono<Void>> handler
);
```

`Subscription` 使用 `String[] topics` 保存静态 Topic。大量 Topic 只有一个路径段
不同时，例如：

```text
/org/o1/device/**
/org/o2/device/**
...
/org/o50000/device/**
```

现有实现仍会为每个值注册完整路径；权限范围变化时，调用方只能取消旧订阅再
重建全部 Topic。普通 `/org/*/device/**` 加业务层过滤会让无关消息先进入订阅者，
集群场景还会产生无关的跨节点传输，不能作为等价优化。

本设计将 #90 与 #91 在 `jetlinks-core` 合并为同一套通用 SPI：

1. 使用不可变 `SubscriptionPlan` 表达完整订阅快照。
2. 使用 `TopicSubscriptionPlan` 表达多个 Route 的 OR 组合。
3. 使用 `IndexedTopicRoute` 表达“公共 pattern + 某一段的精确值集合”。
4. 返回长期 `EventSubscription` 句柄，调用方提交完整新 Plan，差量计算、版本、
   原子切换均由实现负责。
5. 直接在 `EventBus` 增加 `subscribe(plan)` 与 `subscribe(plan, handler)`，不再引入
   `UpdatableEventBus` 平行接口。
6. handler 继续使用 `Function<TopicPayload, Mono<Void>>`。生产者 Reactor Context
   通过返回的 `Mono` 链传播，需要读取时使用 `Mono.deferContextual(...)`，不增加
   `BiFunction<ContextView, ...>` 重载。

本设计面向通用 Topic/EventBus 能力，不包含 AssetsHolder、租户、组织等业务特调。

## 2. 设计目标

1. 以 `O(valueCount + patternCount)` 量级保存集合段订阅，不再按
   `valueCount × 重复后缀深度` 建树。
2. 命中和未命中查找均不能扫描全部 Registration。
3. 更新期间保持同一个下游 Flux、handler 和订阅身份，不重建未变化路由。
4. 调用方只提交完整不可变 Plan，不维护 added/removed，也不提供 generation。
5. 更新在本地具有明确线性化点；迟到的旧更新不能恢复旧范围。
6. 已入 buffer 的消息在 converter、`onNext` 或 handler 前按当前 Plan 重检。
7. 保持 Reactive Streams 的单次 `onSubscribe`、背压、取消、discard 和 Context
   语义。
8. 保持已有 `Subscription` API、构造器和 `Externalizable` 字节格式不变。
9. 不支持 structured Plan 的实现必须显式失败，不能静默退化成无约束 wildcard。
10. 新增公共类和方法统一使用真实版本 `@since 1.2.6`，并补全 SPI Javadoc。

## 3. 影响范围与 owning module

owning repo：`jetlinks-core`。本期只开发 core 公共契约，不进入 supports、Components
或任一 EventBus 具体实现。

本期“契约”包含：

1. 公共 SPI 接口、方法签名、Javadoc、`@since` 和关联类型。
2. 作为公共参数或返回值所必需的不可变值对象、builder、工厂方法和资源限制模型。
3. 为保证值对象自身语义闭合所必需的标准化、参数校验、值语义和 Topic 匹配逻辑。
4. EventBus default 方法的显式不支持行为，以及旧 API、序列化格式和 Java 8 编译
   兼容测试。

本期“契约”不包含运行时基础设施：不维护注册索引，不执行真实消息投递，不实现
更新并发控制、buffer/backpressure、跨边界 Context 传播、编解码 wire format 或
集群同步。这些行为仅在 SPI Javadoc 中固化，留给后续具体实现验证。

主要入口：

- `src/main/java/org/jetlinks/core/event/EventBus.java`
- `src/main/java/org/jetlinks/core/event/Subscription.java`
- `src/main/java/org/jetlinks/core/event/TopicPayload.java`
- `src/main/java/org/jetlinks/core/topic/Topic.java`
- `src/main/java/org/jetlinks/core/topic/TopicFinder.java`
- `src/main/java/org/jetlinks/core/utils/TopicUtils.java`

计划新增：

- `org.jetlinks.core.event.SubscriptionPlan`
- `org.jetlinks.core.event.EventSubscription`
- `org.jetlinks.core.event.EventStream`
- `org.jetlinks.core.event.SubscriptionUpdateResult`
- `org.jetlinks.core.event.SubscriptionSynchronization`
- `org.jetlinks.core.topic.TopicRoute`
- `org.jetlinks.core.topic.PatternTopicRoute`
- `org.jetlinks.core.topic.IndexedTopicRoute`
- `org.jetlinks.core.topic.TopicSubscriptionPlan`
- `org.jetlinks.core.topic.TopicRouteTable`
- `org.jetlinks.core.topic.TopicRouteRegistration`
- `org.jetlinks.core.topic.TopicRouteTableMetrics`
- `org.jetlinks.core.topic.TopicSubscriptionPlanCodec`
- `org.jetlinks.core.topic.TopicSubscriptionPlanDecodeLimits`

## 4. 非目标

1. 不实现 `InternalEventBus`、`ClusterEventBus` 或其他具体 EventBus，也不新增用于
   演示行为的内存 EventBus。
2. 不设计集群 Envelope、RPC、能力协商、重连、tombstone 或同步重试；集群计划
   后续在 Components owning module 单独编写。
3. 不决定业务层如何计算 allowed values。
4. 不通过 TTL 注销低频 Topic；没有 retained/replay 时会漏掉恢复后的首条消息。
5. 不把 wildcard 后置过滤包装成 indexed route。
6. 第一版不支持一个 `IndexedTopicRoute` 中的多个 indexed segment。
7. 不允许动态修改 subscriber、features、priority、time 或本地回调；这些属性变化
   需要创建新订阅。
8. 第一阶段不提供 `TopicRouteTable` 默认索引或内存实现、具体 codec、MBean、
   tracing 或性能结论。

## 5. 公共 API 决策

### 5.1 直接扩展 EventBus

最终建议直接在 `EventBus` 增加两个重载：

```java
public interface EventBus {

    /**
     * 使用结构化 Plan 创建可动态更新的事件流。
     *
     * @param plan 初始完整订阅快照，不能为 null
     * @return 可更新、可取消的单下游事件流
     * @throws UnsupportedOperationException 当前实现不支持结构化订阅时抛出
     * @see SubscriptionPlan
     * @see EventStream
     * @since 1.2.6
     */
    default EventStream<TopicPayload> subscribe(SubscriptionPlan plan) {
        throw new UnsupportedOperationException(
            "structured subscription plan is not supported"
        );
    }

    /**
     * 使用结构化 Plan 创建 handler 订阅。
     *
     * handler Mono 必须由实现组合到消息投递链，不能脱离生命周期启动。
     *
     * @param plan 初始完整订阅快照，不能为 null
     * @param handler 非阻塞处理器，不能为 null，也不能返回 null
     * @return 可更新、可取消的订阅句柄
     * @throws UnsupportedOperationException 当前实现不支持结构化订阅时抛出
     * @see EventSubscription
     * @since 1.2.6
     */
    default EventSubscription subscribe(
        SubscriptionPlan plan,
        Function<TopicPayload, Mono<Void>> handler
    ) {
        throw new UnsupportedOperationException(
            "structured subscription plan is not supported"
        );
    }
}
```

选择直接扩展 `EventBus` 的原因：

1. 动态更新是订阅生命周期的一部分，不应要求调用方先判断或转换到另一套 EventBus。
2. `Subscription` 与 `SubscriptionPlan` 参数类型不同，不依赖返回类型重载，不存在
   方法签名冲突。
3. Java 8 `default` 方法可保护已有第三方 `EventBus` 实现的二进制兼容；具体实现
   后续在各自 owning module 单独设计。
4. 默认实现必须显式抛出 `UnsupportedOperationException`。不能把 indexed route
   展开成 wildcard，也不能返回一个表面可更新、实际取消重建的伪句柄。

如果未来进入允许破坏 SPI 的主版本，可将这两个方法改为抽象方法。第一版不新增
`UpdatableEventBus`、capability cast 或重复命名的 `subscribeUpdatable`。

已有 API 全部保留：

```java
Flux<TopicPayload> subscribe(Subscription subscription);

<T> Flux<T> subscribe(Subscription subscription, Class<T> type);

Cancelable subscribe(
    Subscription subscription,
    Function<TopicPayload, Mono<Void>> handler
);
```

### 5.2 句柄分层

流式订阅和 handler 订阅共享控制能力，但 handler 订阅不暴露无意义的 Flux：

```java
/**
 * 可动态更新的 EventBus 订阅生命周期句柄。
 *
 * 实现必须以完整不可变快照作为更新边界，并保证 updatePlan 与 dispose
 * 对同一句柄具有确定的线性化顺序。释放后不得被迟到更新复活。
 *
 * @see SubscriptionPlan
 * @since 1.2.6
 */
public interface EventSubscription extends Cancelable {

    /**
     * 返回当前本地已经生效的不可变订阅快照。
     *
     * @return 当前 Plan，不会返回 null
     * @since 1.2.6
     */
    SubscriptionPlan getPlan();

    /**
     * 原子替换完整 Plan。
     * 返回的 Mono 被多次订阅时不得重复推进 revision 或重复修改路由。
     *
     * @param nextPlan 下一完整快照；固定属性必须与初始 Plan 一致
     * @return 更新结果；Mono 完成时本地切换必须已经生效
     * @throws IllegalArgumentException Plan 非法或固定属性发生变化
     * @throws IllegalStateException 句柄已经释放
     * @since 1.2.6
     */
    Mono<SubscriptionUpdateResult> updatePlan(SubscriptionPlan nextPlan);
}
```

```java
/**
 * 同时暴露事件 Flux 和更新句柄的结构化订阅。
 *
 * @param <T> 事件元素类型
 * @see EventSubscription
 * @since 1.2.6
 */
public interface EventStream<T> extends EventSubscription {

    /**
     * 返回单下游事件流。同一个 EventStream 只允许一个下游订阅。
     *
     * @return 与句柄共享 Plan 和取消生命周期的事件流
     * @since 1.2.6
     */
    Flux<T> flux();
}
```

生命周期约束：

- `EventStream` 在 `flux()` 被订阅时激活路由，保持现有 Flux API 的订阅时机。
- 在激活前调用 `updatePlan`，只更新首次激活使用的快照。
- handler 重载在 `EventBus.subscribe(plan, handler)` 返回前建立本地订阅。
- `EventStream.flux()` 只允许一个下游，第二个下游收到 duplicate subscription 错误。
- `dispose/cancel` 首先关闭本地交付门禁，再清理索引和其他外部状态。
- dispose 后的更新必须失败，且不能让迟到更新重新激活订阅。
- 流取消后不额外发送 `onComplete`，遵循 Reactive Streams cancel 语义。

### 5.3 更新结果

项目当前使用 Java 8，不能使用 `record`。结果类型使用普通不可变类：

```java
public final class SubscriptionUpdateResult {

    private final long revision;
    private final boolean changed;
    private final SubscriptionSynchronization synchronization;

    public SubscriptionUpdateResult(
        long revision,
        boolean changed,
        SubscriptionSynchronization synchronization
    );

    public long getRevision();

    public boolean isChanged();

    public SubscriptionSynchronization getSynchronization();
}
```

```java
public enum SubscriptionSynchronization {
    LOCAL_APPLIED,
    CLUSTER_SYNCHRONIZED,
    DEGRADED
}
```

状态语义：

- `LOCAL_APPLIED`：本地已切换；该订阅不需要外部同步。
- `CLUSTER_SYNCHRONIZED`：本地已切换，实现声明要求的外部订阅状态也已同步。
- `DEGRADED`：本地已切换，但外部同步未完全成功；恢复方式由具体实现定义。

`STAGED` 不进入公共结果，因为成功完成的 update Mono 不应返回“尚未完成本地
切换”的中间状态。

同一个 Plan 的幂等更新返回当前 revision、`changed=false`，不重复改索引或广播。
本地校验、索引构造或预安装失败时 `Mono` 以 error 结束，旧 Plan 保持有效。
外部同步失败发生在本地切换之后时不能伪装回滚，可由实现返回 `DEGRADED`。

## 6. SubscriptionPlan

`SubscriptionPlan` 是一次订阅的完整不可变快照，位于
`org.jetlinks.core.event`：

```java
/**
 * EventBus 结构化订阅的完整不可变快照。
 *
 * subscriber、features、priority 和 time 确定订阅身份及投递语义；
 * routePlan 表示可动态更新的 Topic 范围。本地回调不参与值语义或编码。
 *
 * @see Subscription
 * @see TopicSubscriptionPlan
 * @since 1.2.6
 */
public final class SubscriptionPlan {

    public static Builder builder(String subscriber);

    public static SubscriptionPlan from(Subscription subscription);

    public String getSubscriber();

    public TopicSubscriptionPlan getRoutePlan();

    public Set<Subscription.Feature> getFeatures();

    public int getPriority();

    public long getTime();

    public SubscriptionPlan withRoutes(
        TopicSubscriptionPlan routePlan
    );

    public void discard(TopicPayload payload);

    public void dropped(TopicPayload payload);

    public static final class Builder {

        public Builder routes(TopicSubscriptionPlan routePlan);

        public Builder features(Subscription.Feature... features);

        public Builder priority(int priority);

        public Builder time(long time);

        public Builder doOnSubscribe(Runnable listener);

        public Builder onDropped(Consumer<TopicPayload> listener);

        public SubscriptionPlan build();
    }
}
```

约束：

1. subscriber、features、priority、time 和本地 callback 在句柄生命周期内固定，
   只允许 `routePlan` 更新。
2. `updatePlan(next)` 必须验证固定字段与初始 Plan 相同；不同则以参数错误结束，
   提示调用方新建订阅。
3. routes、features、allowed values 均做防御性复制，对外只暴露只读视图。
4. `doOnSubscribe`、drop listener 仅在本地使用，不参与编码或
   `equals/hashCode/toString`，`withRoutes` 保留原回调引用。
5. `SubscriptionPlan.from(subscription)` 将已有 `String[] topics` 转为
   `PatternTopicRoute`，并保留已有 feature、priority、time 和本地回调语义。
6. 不修改 `Subscription` 字段、构造器、builder 或
   `writeExternal/readExternal`。旧对象和旧字节流继续按原格式工作。
7. `TopicSubscriptionPlan.empty()` 是合法范围，表示句柄仍存在但不接收消息；这对
   权限被清空时的原子更新是必需的。

## 7. Route 模型

### 7.1 TopicRoute

项目当前以 Java 8 编译，因此不使用 sealed interface：

```java
/**
 * 一个不可变 Topic 匹配规则。
 *
 * Route 负责最终精确判定；索引只可用其结构定位候选，不能替代 matches。
 * 实现必须线程安全，且不得在 matches 中执行阻塞操作。
 *
 * @see PatternTopicRoute
 * @see IndexedTopicRoute
 * @since 1.2.6
 */
public interface TopicRoute {

    /** 返回标准化后的 Topic pattern。 */
    String getPattern();

    /** 对完整 Topic 做最终精确判定。 */
    boolean matches(SeparatedCharSequence topic);
}
```

第一版内置并保证模型、匹配与编码兼容的实现只有：

- `PatternTopicRoute`：现有 exact、`*`、`**` 语义。
- `IndexedTopicRoute`：一个 pattern 中一个命名段对应一组精确允许值。

第一版不把 `TopicRoute` 当成任意第三方 Route 类型的开放序列化 SPI。RouteTable 或
codec 收到未知实现时必须显式失败，不能猜测其语义。

### 7.2 PatternTopicRoute

```java
/**
 * 与现有 TopicFinder 语义一致的 exact、`*`、`**` Topic Route。
 *
 * @see TopicFinder
 * @see TopicUtils
 * @since 1.2.6
 */
public final class PatternTopicRoute implements TopicRoute {

    public static PatternTopicRoute of(String pattern);

    @Override
    public String getPattern();

    @Override
    public boolean matches(SeparatedCharSequence topic);
}
```

`PatternTopicRoute` 使用与 `TopicFinder`/`TopicUtils` 一致的 exact、`*`、`**`
匹配规则。构造时完成标准化和语法校验，运行时不重复解析 pattern。

### 7.3 IndexedTopicRoute

```java
/**
 * 对 pattern 中一个命名路径段应用精确允许值集合的 Topic Route。
 *
 * @see TopicRoute
 * @see TopicSubscriptionPlan
 * @since 1.2.6
 */
public final class IndexedTopicRoute implements TopicRoute {

    public static IndexedTopicRoute of(
        String pattern,
        String indexedVariable,
        Collection<? extends CharSequence> allowedValues
    );

    @Override
    public String getPattern();

    public String getIndexedVariable();

    public int getIndexedSegment();

    public Set<String> getAllowedValues();

    public IndexedTopicRoute withAllowedValues(
        Collection<? extends CharSequence> allowedValues
    );

    @Override
    public boolean matches(SeparatedCharSequence topic);
}
```

使用示例：

```java
IndexedTopicRoute route = IndexedTopicRoute.of(
    "/org/{orgId}/device/**",
    "orgId",
    allowedOrgIds
);
```

第一版约束：

1. pattern 中必须且只能有一个与 `indexedVariable` 同名的完整段，例如
   `{orgId}`。
2. indexed segment 之前不允许出现 `**`，保证目标段位置固定；之后可以使用
   `*` 或 `**`。
3. allowed value 是完整 Topic segment 的精确值，不能为空、不能包含 `/`，也不能
   使用 `*`、`**` 或 `{...}` 伪装通配符。
4. allowed values 构造时去重并防御性复制；空 Set 合法，表示不匹配任何 Topic。
5. 授权判定必须做完整字符串相等比较。hash 只能用于定位 bucket，Bloom Filter
   不能作为最终判断。
6. Topic 段缺失、位置越界、pattern 不匹配或值不在 Set 中均返回 false。
7. 一个 Plan 可包含多个 Indexed Route，因此第一版支持多个 pattern，但每个 Route
   只索引一个段。多 indexed segment 留待有真实场景和性能数据后扩展。

### 7.4 TopicSubscriptionPlan

```java
/**
 * 多个 TopicRoute 的不可变 OR 组合。
 *
 * @see TopicRoute
 * @see SubscriptionPlan
 * @since 1.2.6
 */
public final class TopicSubscriptionPlan {

    public static TopicSubscriptionPlan empty();

    public static TopicSubscriptionPlan of(TopicRoute... routes);

    public static TopicSubscriptionPlan of(
        Collection<? extends TopicRoute> routes
    );

    public static TopicSubscriptionPlan fromTopics(
        Collection<? extends CharSequence> topics
    );

    public List<TopicRoute> getRoutes();

    public boolean isEmpty();

    /** 多个 Route 按 OR 语义匹配。 */
    public boolean matches(SeparatedCharSequence topic);
}
```

构造时对相同 Route 去重并形成稳定顺序，使输入顺序不同但语义相同的 Plan 可以
幂等比较和确定性编码。同一消息同时命中一个 Plan 内多个 Route 时，一个
Registration 最多返回和交付一次。

`fromTopics(...)` 用于从旧 Topic 列表迁移，并保持现有 Topic 展开规则；需要精确
集合索引时必须显式创建 `IndexedTopicRoute`，不能把 `{variable}` 自动理解成权限
集合。

## 8. TopicRouteTable SPI

RouteTable 是同步、内存内的底层结构。它不包装 `Mono`，也不处理网络；
`EventSubscription` 在上层组合生命周期和响应式结果。外部同步由具体实现负责。

```java
/**
 * Topic Route 的并发候选索引 SPI。
 *
 * find 只负责定位候选；交付前仍需调用 Registration.matches 执行当前
 * 快照的最终门禁。实现不能在每次 find 时扫描全部 Registration。
 *
 * @param <T> Registration 关联目标类型
 * @see TopicRouteRegistration
 * @since 1.2.6
 */
public interface TopicRouteTable<T> {

    /**
     * 注册目标及其初始完整 Route Plan。
     *
     * @param target 关联目标，不能为 null
     * @param plan 初始不可变 Plan，不能为 null，可以为空
     * @return 独立、可更新、可释放的 Registration
     * @throws IllegalArgumentException target、Plan 或 Route 不受支持
     * @since 1.2.6
     */
    TopicRouteRegistration<T> register(
        T target,
        TopicSubscriptionPlan plan
    );

    /**
     * 使用已分段 Topic 同步查找候选 Registration。
     *
     * 同一 Registration 即使被多个 Route 命中，也只能回调一次。
     *
     * @param topic 完整已分段 Topic，不能为 null
     * @param consumer 候选接收器，不能为 null
     * @since 1.2.6
     */
    void find(
        SeparatedCharSequence topic,
        Consumer<? super TopicRouteRegistration<T>> consumer
    );

    /**
     * 使用 CharSequence Topic 同步查找候选 Registration。
     *
     * @param topic 完整 Topic，不能为 null
     * @param consumer 候选接收器，不能为 null
     * @since 1.2.6
     */
    void find(
        CharSequence topic,
        Consumer<? super TopicRouteRegistration<T>> consumer
    );

    /**
     * 返回路由表的有界只读统计快照。
     *
     * @return 统计快照，不暴露内部集合
     * @since 1.2.6
     */
    TopicRouteTableMetrics metrics();
}
```

```java
/**
 * 一个目标在 TopicRouteTable 中的长期注册句柄。
 *
 * 同一 Registration 的 updatePlan 与 dispose 必须线性化；dispose 后
 * matches 永远为 false，更新不能重新激活已释放句柄。
 *
 * @param <T> 关联目标类型
 * @see TopicRouteTable
 * @since 1.2.6
 */
public interface TopicRouteRegistration<T> extends Disposable {

    /**
     * @return 注册时关联的目标，不会返回 null
     * @since 1.2.6
     */
    T getTarget();

    /**
     * @return 当前不可变 Route Plan，不会返回 null
     * @since 1.2.6
     */
    TopicSubscriptionPlan getPlan();

    /**
     * @return 当前 Plan revision；初始值为 0
     * @since 1.2.6
     */
    long getRevision();

    /**
     * 同步原子替换当前完整 Route Plan。
     *
     * 相同 Plan 不增加 revision。失败时抛出异常并保持旧 Plan。
     *
     * @param nextPlan 下一完整不可变 Plan，不能为 null
     * @return 更新后的当前 revision
     * @throws IllegalArgumentException Plan 或 Route 不受支持
     * @throws IllegalStateException Registration 已释放
     * @since 1.2.6
     */
    long updatePlan(TopicSubscriptionPlan nextPlan);

    /**
     * 使用当前快照执行最终匹配门禁。
     *
     * @param topic 完整已分段 Topic，不能为 null
     * @return 当前未释放且 Plan 匹配时返回 true
     * @since 1.2.6
     */
    boolean matches(SeparatedCharSequence topic);
}
```

```java
/**
 * TopicRouteTable 的有界只读统计快照。
 *
 * @since 1.2.6
 */
public interface TopicRouteTableMetrics {

    /**
     * @return 当前未释放 Registration 数量
     * @since 1.2.6
     */
    long getRegistrationCount();

    /**
     * @return 当前 pattern group 数量
     * @since 1.2.6
     */
    long getPatternGroupCount();

    /**
     * @return 当前精确索引值与 Registration 关联数量
     * @since 1.2.6
     */
    long getIndexedValueCount();

    /**
     * @return 实际改变 Plan 的成功更新次数
     * @since 1.2.6
     */
    long getUpdateSuccessCount();

    /**
     * @return 更新失败次数
     * @since 1.2.6
     */
    long getUpdateFailureCount();
}
```

`TopicRouteTableMetrics` 只描述 Registration、索引结构和 Route Plan 更新，不包含
消息投递计数。因 Plan 更新而被最终门禁拒绝的消息属于 EventBus 投递层指标，后续
具体 EventBus 实现可在自身 metrics 或 MBean 中暴露，不能反向耦合进 RouteTable SPI。

SPI 契约：

1. `find` 只定位候选 Registration，并保证同一 Registration 最多回调一次。
2. 交付方仍须在真正交付前调用当前 Registration 的最终门禁，不能把候选命中
   当作授权结论。
3. `find`、`matches` 是并发读热路径，不进行阻塞 IO，不扫描全部
   Registration。
4. `updatePlan` 与 `dispose` 对同一 Registration 串行化；不同 Registration 可并发。
5. dispose 后 `getPlan` 可返回最后快照用于诊断，但 `matches` 永远为 false，更新
   不能复活 Registration。

revision 契约：

1. 注册成功后的初始 revision 为 `0`。
2. 每次实际改变 Plan 时加一；相同 Plan 不增加。
3. revision 只在 Registration 生命周期内单调，不承诺跨进程或重建后全局唯一。
4. `getPlan` 与 `getRevision` 必须来自同一原子快照，不能暴露错配组合。

### 8.1 后续索引实现约束

后续默认实现应按标准化 pattern 建 `PatternGroup`：

- pattern 的公共前缀和后缀只保存一次，并进入与 `TopicFinder` 语义一致的 pattern
  索引。
- indexed segment 对应 `value -> registrations` 精确倒排表。
- 一个 Registration 的当前不可变 Plan 是最终门禁；倒排表只负责缩小候选集。
- 普通 exact/`*`/`**` Route 与 indexed PatternGroup 可共用上层 pattern 搜索结构。
- 空 value bucket 和无 Registration 的 PatternGroup 在更新完成后回收。

因此查找成本与实际命中的 pattern group 和 value bucket 相关，而不是与全局订阅
数量相关；常驻结构为 `O(valueCount + patternCount + registrationCount)`。

## 9. 完整 Plan 更新与原子性

调用方始终更新完整 Plan：

```java
SubscriptionPlan next = subscription
    .getPlan()
    .withRoutes(nextRoutePlan);

Mono<SubscriptionUpdateResult> result = subscription.updatePlan(next);
```

不公开 added/removed API，原因如下：

1. 调用方通常持有的是最新权限快照，要求其同时维护正确 Delta 会增加复杂度。
2. 完整快照天然支持重试，不依赖任何中间增量都不丢失。
3. 实现可按 Route 稳定键和 allowed value Set 计算最小索引差量。

### 9.1 本地更新算法

对同一 Registration：

1. 校验 next Plan 的固定字段和 Route 语法，构造完整不可变快照。
2. 与当前 Plan 比较；完全相同则返回 `changed=false`。
3. 进入 Registration 级串行区，确认未 dispose，并分配下一个内部 revision。
4. 计算 old/new Route 差异，预创建新 PatternGroup，并预装新增 pattern/value 候选。
5. 原子切换 current Snapshot。这是更新的线性化点。
6. 删除旧 pattern/value 候选，回收空 value bucket 和空 PatternGroup。
7. 返回本地 revision；外部同步状态由具体 EventBus 实现组合。

该顺序保证：

- 切换前，新候选即使已进入索引，也会被旧 Snapshot 最终门禁拒绝。
- 切换后，旧候选即使尚未清理，也会被新 Snapshot 最终门禁拒绝。
- 预安装或构建失败时尚未切换，旧 Plan 完整保留。
- 切换后清理失败时新 Plan 仍有效；残留候选不能越过门禁，并进入清理重试。
- dispose 先切换为 disposed/empty 门禁，再清理索引；迟到更新不能复活订阅。

Plan 构造和完整 Set diff 至少需要 `O(totalValues)`；实际索引插入/删除为
`O(delta)`。性能报告必须区分这两部分，不能宣称整个更新都是 `O(delta)`。

### 9.2 交付与更新的并发边界

每次交付在 converter、`onNext` 或 handler 之前执行 `tryEnter(topic)`：

1. 在与 Plan 切换一致的并发边界内读取当前 Snapshot。
2. 精确匹配后登记为“已开始交付”。
3. 更新只等待完成 Snapshot 切换，不等待已开始 handler 退出。

语义为：

- 更新线性化之前已经开始的 handler 可以完成。
- 更新线性化之后，不得再启动旧范围的新 handler 或 `onNext`。
- handler 可以直接返回同一句柄的 `updatePlan(...)` Mono；更新不能等待当前 handler
  自身结束，否则会形成自等待死锁。

并发调用多个 `updatePlan` 时，实现生成单调 revision 并形成确定的串行顺序。
调用方不传 generation；每个返回结果携带实际 revision，用于诊断和实现内部排序。

## 10. Buffer 与 Reactive Streams

仅把 `TopicPayload` 放进 buffer 不足以在更新后重检。新元素至少保留命中的
Registration 和生产者 Context：

```java
final class BufferedEvent<T> {

    private final TopicRouteRegistration<T> registration;
    private final ContextView context;
    private final TopicPayload payload;
}
```

drain 规则：

1. 从 buffer poll 后，先按 Registration 当前 Snapshot 重检。
2. 通过重检后才能执行 converter、`onNext` 或 handler。
3. `polled` 表示实际从 buffer 移除的数量，`emitted` 表示真正发给下游的数量，
   两个计数不得混用。
4. buffer occupancy 按 `polled` 减少；downstream demand 只按 `emitted` 减少。
5. 因 Plan 更新被拒绝的消息不消耗 demand，drain 应继续寻找下一条可交付消息。
6. 更新范围导致的丢弃调用 `SubscriptionPlan.discard(payload)` 并执行
   `Operators.onDiscard`；buffer overflow 调用 `SubscriptionPlan.dropped(payload)`。
7. cancel 清空 buffer、执行 discard 并释放 Registration；不能在 cancel 后额外发送
   `onComplete`。
8. Context 只在消息排队期间被引用，poll/discard/cancel 后及时释放 buffer 元素，
   避免长期保留生产者上下文中的大对象。

需要用有限 demand 专门验证“连续多条已失效消息 + 一条有效消息”的场景，确保
discard 不错误扣减 request，也不会因 occupancy 未减少而卡住 drain。

## 11. Handler 与生产者 Context

handler API 保持简单：

```java
EventSubscription subscription = eventBus.subscribe(
    plan,
    payload -> handle(payload)
);
```

需要感知生产者 Context 时，直接在返回的 Mono 中延迟读取：

```java
EventSubscription subscription = eventBus.subscribe(
    plan,
    payload -> Mono.deferContextual(context ->
        handle(context, payload)
    )
);
```

不增加 `(subscription, handler)` 专用函数类型或
`BiFunction<ContextView, TopicPayload, Mono<Void>>` 公共重载，原因是 Reactor 已通过
`Mono.deferContextual` 提供标准 Context 感知方式；额外重载会扩大 API 面并诱导
实现提前读取错误的 Context。

实现约束：

1. handler 返回的 Mono 必须组合进 EventBus 发布/投递链。
2. 不得使用脱离发布生命周期的业务 `subscribe()` 来启动 handler。
3. 不得用 `Context.empty()` 覆盖生产者 Context。
4. 本地异步切换线程时保持 Reactor Context；不使用 ThreadLocal 自行传播。
5. 跨边界 Context 如何编码和恢复不属于 core handler SPI；具体实现不能假设任意
   Reactor Context 对象可以直接跨进程传播。

## 12. 编码 SPI 与安全限制

第一阶段只固化 codec 边界，不固化具体二进制格式。`version()` 由具体 codec
实现声明，格式版本是 payload 外部元数据，由调用方选择对应 codec 后再解码。

```java
/**
 * TopicSubscriptionPlan 的版本化二进制编解码 SPI。
 *
 * codec 只处理 Route Plan，不处理 subscriber、features、revision 或上层
 * Envelope。实现必须在分配集合或字符串前应用调用方提供的 limits。
 *
 * @see TopicSubscriptionPlan
 * @see TopicSubscriptionPlanDecodeLimits
 * @since 1.2.6
 */
public interface TopicSubscriptionPlanCodec {

    /**
     * @return 此 codec 支持的正整数格式版本
     * @since 1.2.6
     */
    int version();

    /**
     * 以确定性顺序编码完整 Route Plan。
     *
     * @param plan 待编码 Plan，不能为 null
     * @param output 目标输出，生命周期由调用方管理
     * @throws IOException 写入失败或 Route 类型不受支持
     * @since 1.2.6
     */
    void encode(
        TopicSubscriptionPlan plan,
        DataOutput output
    ) throws IOException;

    /**
     * 解码并校验完整 Route Plan。
     *
     * 未知 Route 类型、非法长度或超过 limits 时必须显式失败。
     *
     * 调用方必须先根据外部 frame 元数据选择对应版本的 codec，并把 input 限定在
     * 当前 payload 的精确边界内。实现必须在任何集合或字符串分配前校验
     * payloadLength。
     *
     * @param input 当前 payload 的有界输入，生命周期由调用方管理
     * @param payloadLength 当前 payload 字节长度，不能为负数
     * @param limits 本次解码的强制资源限制，不能为 null
     * @return 完整不可变 Plan
     * @throws IOException 输入非法、截断、不支持或超过限制
     * @since 1.2.6
     */
    TopicSubscriptionPlan decode(
        DataInput input,
        int payloadLength,
        TopicSubscriptionPlanDecodeLimits limits
    ) throws IOException;
}
```

```java
/**
 * Route Plan 解码时由调用方显式提供的资源上限。
 *
 * core 不提供猜测性的业务默认值。
 *
 * @since 1.2.6
 */
public final class TopicSubscriptionPlanDecodeLimits {

    public TopicSubscriptionPlanDecodeLimits(
        int maxRoutes,
        int maxIndexedValues,
        int maxValueLength,
        int maxPayloadLength
    );

    public int getMaxRoutes();

    public int getMaxIndexedValues();

    public int getMaxValueLength();

    public int getMaxPayloadLength();
}
```

后续具体 codec 必须满足：

1. 编码包含 Route type、标准化 pattern、indexed variable 和 values；codec version
   由调用方在 payload 外部携带并选择对应 codec。未知 version 必须在调用 decode
   前显式失败，不能猜测或自动降级。
2. Route 与 value 使用确定性顺序，相同 Plan 生成稳定字节序列。
3. decode 先校验 `payloadLength >= 0` 且不超过 `maxPayloadLength`，再在分配集合前
   检查 route 数、allowed value 总数和单值长度。
4. 未知 codec version、未知 Route type、负数/溢出数量、非法 pattern 或截断 payload
   均显式失败。
5. 限制同时作用于单 Route 和完整 Plan，防止多个 Route 绕过总量限制。
6. 调用方必须在读取 frame 前执行 `maxPayloadLength` 门禁，并向 decode 传入实际
   `payloadLength` 及限定到该 frame 的 `DataInput`；codec 仍须独立执行相同上限
   校验，不能在已分配完整 payload 后才检查。
7. core 不提供猜测性的默认阈值。上层实现必须结合自身 frame、内存限制和真实
   数据规模构造 `TopicSubscriptionPlanDecodeLimits`。

`TopicSubscriptionPlanCodec` 只负责 Route 模型，不负责 subscriber、features、
revision 或任何上层传输 Envelope。

第一阶段固化 codec 接口与 limits 模型，不提交具体二进制格式。具体 codec 实现和
兼容向量待出现明确上层传输需求后单独设计。

## 13. 错误与边界语义

| 场景 | 结果 |
|---|---|
| Plan 语法或固定字段非法 | 更新 Mono error，旧 Plan 不变 |
| 相同 Plan | `changed=false`，不增加 revision |
| 本地预安装失败 | 更新 Mono error，旧 Plan 不变 |
| 本地切换成功、清理失败 | 新 Plan 有效，残留候选被门禁拒绝并重试 |
| 本地切换成功、外部同步失败 | 具体实现可返回 `DEGRADED` |
| 更新与 dispose 并发且 dispose 先线性化 | 更新失败，订阅不能复活 |
| 更新与 dispose 并发且更新先线性化 | 更新可完成，随后 dispose 关闭门禁 |
| buffer 中消息已不属于当前 Plan | discard，不消耗 downstream demand |
| 同一 Plan 内多个 Route 同时命中 | Registration 只交付一次 |
| 未知 Route/codec 版本 | 显式失败，不使用 wildcard fallback |

内部日志和异常不是用户可见业务文案，本次不新增 i18n 资源。

## 14. 使用示例

### 14.1 流式订阅

```java
TopicSubscriptionPlan routes = TopicSubscriptionPlan.of(
    IndexedTopicRoute.of(
        "/org/{orgId}/device/**",
        "orgId",
        allowedOrgIds
    ),
    PatternTopicRoute.of("/system/config/**")
);

SubscriptionPlan plan = SubscriptionPlan
    .builder("device-state-cache")
    .routes(routes)
    .features(
        Subscription.Feature.local,
        Subscription.Feature.broker
    )
    .build();

EventStream<TopicPayload> stream = eventBus.subscribe(plan);

Disposable downstream = stream
    .flux()
    .subscribe(this::handlePayload);
```

权限范围变化时保持同一 stream：

```java
TopicSubscriptionPlan nextRoutes = TopicSubscriptionPlan.of(
    IndexedTopicRoute.of(
        "/org/{orgId}/device/**",
        "orgId",
        latestAllowedOrgIds
    )
);

Mono<SubscriptionUpdateResult> update = stream.updatePlan(
    stream.getPlan().withRoutes(nextRoutes)
);
```

### 14.2 Handler 订阅与 Context

```java
EventSubscription subscription = eventBus.subscribe(
    plan,
    payload -> Mono.deferContextual(context ->
        handleWithProducerContext(context, payload)
    )
);
```

handler 若需要触发自身 Plan 更新，直接把 `updatePlan(...)` Mono 返回到当前处理链；
不要在 handler 内再调用 `subscribe()`。

## 15. 第一阶段测试目标

### 15.1 API、Route 与不可变性

1. exact、`*`、`**` 与当前 `TopicFinder`/`TopicUtils` 匹配结果一致。
2. Indexed Route 与展开为 exact topics 的结果完全一致。
3. pattern 缺段、segment 越界、非法变量、indexed 前出现 `**`、空/非法 value。
4. 空 allowed values 与空 Plan 均不匹配。
5. 单 Plan 多 Route 重复命中时只返回一次匹配结果。
6. Route、Plan 和 allowed values 对输入集合防御性复制，对外集合不可修改。
7. Route 输入顺序不同但语义相同时，Plan 等价且具有确定顺序。
8. callback 不参与 SubscriptionPlan 的 equals/hashCode/toString。

### 15.2 兼容与 SPI 契约

1. EventBus 旧方法签名不变；仅实现旧抽象方法的测试实现仍可编译和运行。
2. EventBus 新 default 方法对不支持实现抛出 `UnsupportedOperationException`。
3. 现有 `SubscriptionTest` 保持通过，并冻结旧 Externalizable 历史字节样本。
4. `SubscriptionPlan.from(subscription)` 正确映射 topics、features、priority、time
   和本地回调。
5. Subscription 不新增 structured Plan 字段，不改变 serialVersionUID。
6. `SubscriptionUpdateResult` 和同步状态可由实现方构造和读取。
7. SPI 泛型、重载和 Java 8 编译不存在擦除冲突。

### 15.3 Codec 边界

1. decode limits 拒绝零值、负值和整数边界非法参数。
2. decode 方法显式接收 `payloadLength`，其负值和超过 `maxPayloadLength` 的失败语义
   在公共契约中冻结；调用方负责提供限定在对应 frame 内的 `DataInput`。
3. maxRoutes、maxIndexedValues、maxValueLength、maxPayloadLength 的语义被单元测试冻结。
4. 本阶段不伪造具体二进制格式、协议兼容或性能测试。

### 15.4 后续实现验收条件

以下契约需由后续具体 RouteTable/EventBus 实现验证，第一阶段只在 SPI Javadoc 中
固化，不为尚不存在的实现编写伪测试：

1. 同 Plan 幂等、增删、空 Plan、find/update/dispose 并发和 revision 单调。
2. 未命中不扫描全量 Registration，1k/10k/50k values 的内存和吞吐显著优于 exact
   Topic 展开。
3. buffer 重检、discard 不消耗 demand、cancel 无额外 onComplete。
4. handler `Mono.deferContextual` 可读取生产者 Context，自身更新不死锁。

## 16. 本期 core 契约开发任务与验证

### 16.1 执行原则与顺序

1. 每个任务先新增或更新契约测试，再实现使测试通过；不为通过测试放宽已确认语义。
2. 推荐顺序为 `CORE-SPI-01` → `CORE-SPI-02` → `CORE-SPI-03`；随后可独立执行
   `CORE-SPI-04`、`CORE-SPI-05`、`CORE-SPI-06`；最后执行 `CORE-SPI-07` 和
   `CORE-SPI-08`。
3. 每个任务只固化 core 公共模型和 SPI。测试只验证可执行的值对象语义、API 兼容性
   和签名；运行时并发、投递、索引或编码行为只写入 Javadoc，不创建伪实现来证明。
4. 发现必须依赖具体 EventBus、集群协议、默认索引或 wire format 才能成立时，先
   回写本文并重新确认，不在 core 契约中临时补实现、兼容或降级逻辑。
5. 本期完成前保持 PR 为 Draft；全部门禁通过并补齐 PR 测试证据后再决定是否
   ready for review。

### 16.2 `CORE-SPI-01`：Topic Route 基础模型

- 依赖：无。
- 产物：`TopicRoute`、`PatternTopicRoute`、`IndexedTopicRoute`，以及 pattern
  标准化、语法校验、单 indexed segment 定位和不可变 allowed values。
- 测试：复用真实 `/org/{orgId}/device/**` 形状，覆盖 exact、`*`、`**` 与现有
  `TopicFinder`/`TopicUtils` 的等价性；覆盖缺段、越界、非法变量、indexed 前
  `**`、非法 value、空 allowed values 和防御性复制。
- 完成条件：Route 在构造时完成解析，`matches` 不重复解析 pattern；值语义、稳定
  `equals/hashCode/toString` 和 Java 8 编译通过；公共契约明确未知 `TopicRoute` 不得
  被伪装成内置可编码类型。

### 16.3 `CORE-SPI-02`：TopicSubscriptionPlan

- 依赖：`CORE-SPI-01`。
- 产物：`TopicSubscriptionPlan.empty/of/fromTopics`、OR 匹配、相同 Route 去重和
  确定性顺序。
- 测试：空 Plan、多 Route OR、同一 Topic 多 Route 命中、不同输入顺序等价、重复
  Route 去重、输入集合修改不影响 Plan、返回集合不可修改。
- 完成条件：Plan 是不可变值对象；相同语义形成稳定顺序；`fromTopics` 只创建
  `PatternTopicRoute`，不把 `{variable}` 猜测成 indexed route。

### 16.4 `CORE-SPI-03`：完整 SubscriptionPlan 与旧 Subscription 兼容

- 依赖：`CORE-SPI-02`。
- 产物：`SubscriptionPlan`、builder、`from(Subscription)`、`withRoutes`、固定属性
  校验所需的值语义和本地 callback 保留逻辑。
- 测试：topics、features、priority、time 和 callback 映射；`withRoutes` 仅替换
  Route Plan；callback 不参与 `equals/hashCode/toString`；所有数组和集合防御性
  复制；空 Route Plan 合法。
- 兼容测试：保留现有 `SubscriptionTest`，增加历史 `Externalizable` 字节 fixture，
  冻结 `Subscription` 的字段、`serialVersionUID` 与 `writeExternal/readExternal`
  格式。
- 完成条件：旧 `Subscription` 无生产代码改动；新 Plan 能完整表达旧订阅语义；固定
  属性差异必须被后续 `updatePlan` 明确拒绝的规则已写入 SPI Javadoc。

### 16.5 `CORE-SPI-04`：订阅生命周期与更新结果契约

- 依赖：`CORE-SPI-03`。
- 产物：`EventSubscription`、`EventStream`、`SubscriptionUpdateResult`、
  `SubscriptionSynchronization`。
- 测试：更新结果的 changed、revision、同步状态和诊断信息可稳定构造、读取和比较；
  使用最小编译 fixture 冻结 `EventStream`、可取消句柄和完整 Plan 更新签名。
- 完成条件：异步更新只暴露 `Mono<SubscriptionUpdateResult>`；不公开 delta 或
  generation；公共 Javadoc 明确同 Plan 幂等、revision、更新/dispose 线性化以及
  dispose 后不能重新激活、handler 自更新不得形成自等待。具体 EventBus 行为留给
  后续实现测试，不用测试 fixture 伪造。

### 16.6 `CORE-SPI-05`：TopicRouteTable 扩展点

- 依赖：`CORE-SPI-02`。
- 产物：`TopicRouteTable`、`TopicRouteRegistration`、`TopicRouteTableMetrics`。
- 测试：使用最小编译 fixture 冻结 register/find/updatePlan/matches/dispose 签名、
  泛型和 metrics 读取契约；revision、候选去重和并发语义只在 Javadoc 中固化，不为
  尚不存在的索引实现编写伪行为测试。
- 完成条件：SPI 保持同步内存边界，不引入 Reactor 或网络；metrics 只描述注册、
  索引和 Plan 更新，不包含 EventBus 投递丢弃计数；不提交默认 RouteTable 实现。

### 16.7 `CORE-SPI-06`：Route Plan Codec 扩展点与解码限制

- 依赖：`CORE-SPI-02`。
- 产物：`TopicSubscriptionPlanCodec`、`TopicSubscriptionPlanDecodeLimits`；decode
  签名显式接收 `DataInput`、`payloadLength` 和 limits。
- 测试：limits 拒绝零值、负值和非法整数边界；getter 保持不可变；最小编译 fixture
  冻结 version/encode/decode 签名。payload 长度门禁和分配顺序只在 SPI Javadoc 中
  固化，留给后续具体 codec 的行为测试。
- 完成条件：version 由 payload 外部元数据选择；调用方必须提供 frame 有界输入；
  core 不提供默认阈值、具体二进制格式、自动降级或兼容向量。

### 16.8 `CORE-SPI-07`：EventBus 结构化订阅入口

- 依赖：`CORE-SPI-03`、`CORE-SPI-04`。
- 产物：在 `EventBus` 增加 `subscribe(SubscriptionPlan)` 与
  `subscribe(SubscriptionPlan, Function<TopicPayload, Mono<Void>>)` 两个 default
  重载。
- 测试：只实现旧抽象方法的第三方兼容 fixture 继续编译运行；两个新 default 方法
  默认抛 `UnsupportedOperationException`；handler 函数签名不存在泛型擦除或重载
  歧义。
- 完成条件：旧方法签名完全不变；不新增平行 EventBus 接口或 Context 专用 handler
  类型；Javadoc 明确 handler Mono 必须进入投递链，并以 `Mono.deferContextual`
  感知生产者 Context。

### 16.9 `CORE-SPI-08`：公共契约与质量门禁

- 依赖：`CORE-SPI-01` 至 `CORE-SPI-07`。
- 产物：公共 API Javadoc、统一 `@since 1.2.6`、必要 `@see`、最终测试证据和本文
  实现落点回填。
- 验证命令：`mvn -pl jetlinks-core test`；同时执行 `git diff --check`，核对 Java 8、
  泛型擦除、重载兼容和旧序列化 fixture。
- 完成条件：相关测试报告 0 failed；PR 描述列出测试类、通过/失败/跳过数量与覆盖率
  数据，或明确项目缺少覆盖率工具时的替代证据；本文只回填稳定的代码落点和验证
  摘要，不记录逐步执行日志。

本期不修改 jetlinks-supports 或 Components，不实现具体 EventBus、默认/内存
RouteTable、具体 codec、集群、MBean、tracing 或 benchmark。`SubscriptionPlan` 仅为
不可变模型，本期没有常驻资源和运行时链路，因此不新增 TraceHolder 或 MBean；后续
具体投递、索引和集群实现必须重新评估这两项。完成后只在本文回填 core 契约测试
结果和关键代码路径。

## 17. 已确认决策

1. 第一版每个 `IndexedTopicRoute` 仅一个 indexed segment，且该段之前不允许 `**`。
2. 新公共类型和 SPI 统一使用 `@since 1.2.6`。
3. 本文只包含 jetlinks-core SPI 设计和第一阶段固化工作。
4. 集群实现计划后续单独写入 Components owning module，不在本文预设协议和实现。
5. 如果后续需求改变公共签名、Route 边界或兼容策略，先更新本文并再次确认。
6. 本期只开发 core 契约；不可变值对象及其必要校验、匹配和值语义属于契约，任何
   具体 EventBus、RouteTable、codec 和集群运行时实现均不属于本期。

## 18. 当前交付

- 设计与开发任务提交：`8166b900`。
- Draft PR：[#94 docs(core): 固化动态 Topic 订阅 SPI 设计](https://github.com/jetlinks/jetlinks-core/pull/94)。
- 文档验证：`git diff --check` 通过。
- 生产代码与自动化测试：尚未开始，待开发任务确认后按 `CORE-SPI-01` 至
  `CORE-SPI-08` 执行。
