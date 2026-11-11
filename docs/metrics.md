# 对象指标与属性元数据

通过 public 只读契约与组合注解声明指标，使用 `MetadataUtils` 统一解析属性、类型、扩展与国际化。`MetricSet` 编译一份共享描述与读取器，读取时传入当前运行对象；不持有目标实例、数值副本、窗口或历史数据。

## 声明与读取

```java
public interface QueueMetrics {
    @Gauge
    long getPending();

    @Counter
    long getReceived();

    @Duration(unit = "nanoseconds")
    long getLastDuration();

    @Timestamp
    long getLastSuccess();
}

MetricSet definitions = MetricSet.of(QueueMetrics.class);
ObjectType description = definitions.getMetadata();
PropertyMetadata pending = description.getProperty("pending").orElseThrow();
String name = pending.getI18nName(requestLocale);
String help = pending.getI18nDescription(requestLocale);
// queue 为实现该契约的运行对象；指标读取可能抛出 getter 的原异常。
Object value = definitions.getMetrics().get(0).read(queue);
```

注解位于 `org.jetlinks.core.annotation.metric`。`Counter` 为累计值，默认单位 `count`；`Gauge` 为当前值；`Duration` 与 `Timestamp` 均为 gauge，默认毫秒。耗时与时间戳不自动提供 Timer、速率或 p95。计数的单调性、实例重启边界与事件归属由 owner 保证，读取不清零。

常用业务语义可以继续组合：

```java
@Target({METHOD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Gauge(unit = "count")
@Schema(title = "待执行任务", description = "已入队、尚未开始的任务数")
public @interface PendingTasks {}

public interface WorkerMetrics {
    @PendingTasks
    long getPending();
}
```

底层 `@Metric` 通过既有 `@Expands / @Attr` 描述 kind、semantic 与单位声明。变化属性使用 `@AliasFor` 指向元注解，可选空值使用 `@Attr(ignoreEmpty = true)`；false / 0 仍是有效值。名称与说明复用 `@Schema`，顺序复用 `@Order`，无需在 Metric 中重复填写。

编译后仍使用标准 `PropertyMetadata`：度量语义保存在 `expands.metric`，单位归一到 `valueType.unit`，其他扩展保留，类型、枚举与精度沿用既有 DataType。单位沿用 `ValueUnits` 的注册、JSON 与符号单位行为，例如 `sample` 可作为符号单位，不建立另一份监控单位表；不隐式转换数值尺度。Counter、Duration、Timestamp 要求数值类型；带单位的属性必须支持 `UnitSupported`。累计耗时可用 `@Metric(kind = MetricKind.counter, semantic = MetricSemantic.duration, unit = "milliseconds")` 声明。普通无单位 Gauge 可描述布尔或枚举等状态，外部数值指标导出器应显式选择能导出的类型。

`MetricSet` 按声明契约使用 ClassValue 共享定义，公开 getter 和指标种类只解析一次；实现类上的额外注解不会改变接口契约。只选显式 Schema / 组合 Expands 声明，排除 hidden、static、参数方法、桥接方法与 Object 方法。父子接口的文案可覆盖，同一指标的种类、单位冲突及不同 getter 重用同一 ID 会在解析时失败。返回类型沿用 Java 的覆盖约束，协变声明选择更具体的类型。发现元数据不调用 getter，运行期 getter 应仅读取有界内存状态；读取错误交由消费方表示为 unavailable。

`MetricSet.getMetadata()` 与 `Metric.getMetadata()` 返回同一份共享中立定义，调用方按只读契约使用，不得修改属性、DataType 或扩展。属性集合不支持增删；标准元数据模型本身仍有 setter，此约束不表示整个对象图都不可变。展示或导出时构造自己的响应，按需读取本地化文案，无需为每个请求深复制模型。

## MetadataUtils 公共复用

- `parseProperty(ownerType, method[, id])`：解析方法属性，支持显式 ID，供命令保留原有命名规则；结果按当前语言生成。
- `parsePropertyDefinition(ownerType, method)`：生成中立属性定义，保留默认文案与资源键。
- `parseReadOnlyType(ownerType[, rules])`：显式发现 getter，返回中立 ObjectType；`parseReadOnlyProperties(ownerType, rules)` 另保留进程内方法绑定，避免读取层重复推导属性。
- `Metadata.getI18nName(locale)` / `getI18nDescription(locale)`：按需读取名称与说明，不修改定义。
- `resolveI18nMessage(locale, metadata, key, fallback[, messageSource])`：统一解析内嵌消息与资源键，适用于枚举文本等其他文案；显式消息源不改变进程默认消息源。

现有 `parseType` 继续按字段扫描；Field 与 Method 共用类型、Schema、扩展及文案构造能力，`CommandMetadataResolver` 保留命令输入选择与旧命名规则，委托公共方法入口。嵌套对象、数组与枚举仍使用现有类型体系。现有命令和字段解析的当前语言行为不变，不把其结果直接当成中立模板全局缓存。

接口方法的继承由 Spring 合并注解处理，不能依赖 Java `@Inherited`。解析保留声明层级：普通扩展优先最近声明，局部严格规则可拒绝继承冲突；`ExpandsMerge.with` 从当前对象规则派生局部规则，不更改全局策略。MetricSet 的严格规则仅增加 metric 命名空间，并保留其他已有扩展规则。

## 国际化

中立定义在 `expands.i18nKeys` 保存稳定资源键列表，不保存 Class、Method 或 Locale。属性键沿用 `{声明契约全名}.{属性ID}`，名称优先相同键加 `.title`；还支持父声明与组合注解全名的共享资源回退。即使 getter 只有 `@Counter`，也可通过契约属性的 `.title` 键提供名称；无翻译时保留默认说明或属性 ID。

本地化优先级为：`expands.i18nMessages` 的地区 / 语言消息 → 具体契约资源 → 声明来源 / 组合注解资源 → 默认文案。内嵌消息与资源键统一由 `MetadataUtils.resolveI18nMessage` 解析，资源查询复用 `LocaleUtils`。解析只返回字符串，不修改默认文案、复制模型或缓存 Locale；不同语言并发读取同一份定义不会互相覆盖。

嵌套属性沿用同一入口。枚举在 EnumType 上保留 EnumDict 的资源键，展示边界按元素值读取文本：

```java
String text = MetadataUtils.resolveI18nMessage(
    requestLocale, enumType, element.getValue(), element.getText());
```

## Metrics 生命周期契约

```java
MetricRegistration registration = monitor.metrics()
    .observe(instanceId, QueueMetrics.class, queue);
// registration.isSupported() 区分实际注册与旧实现 / noop 的 unsupported。
// 实例停止时关闭本次句柄；不能通过 scope 重新查询并关闭替换后的新实例。
registration.dispose();
```

`Metrics.observe` 是兼容默认方法：旧 count / value / error 实现、NoopMetrics 返回已关闭的共享 unsupported 句柄，不解析注解或保留 target；不能将 unsupported 当成已采样的零值。ProxyMetrics 在注册时获取一次实际 owner 并返回其句柄，关闭不重新查找 lazy owner。支持实现必须自行校验 target、绑定实例代次、保证幂等注销，并复用 MetricSet；core 不提供对象注册表或采样调度。

注册表、业务埋点、JMX、外部导出与前端由所属模块实现。现有 Recorder / ObservationContext 执行观测保持原契约；本能力没有采集 I/O、后台线程、新增 Trace 或平台 MBean。

## 低成本运行统计

`monitor.metrics` 提供四种独立能力，实例/数组由业务 owner 或插件直接持有，不进入全局注册表，
不自动为采集器、点位或连接创建统计。没有时间序列或逐样本缓存，读取不清零。

| 能力 | 状态与语义 |
| --- | --- |
| `RateMeter` | 一个累计字段和1/5/15分钟 EWMA，共享五秒节拍参数；事件/查询时惰性更新，无定时器 |
| `DurationSummary` | 次数、累计秒、最近/最大纳秒耗时和峰值时刻；固定 primitive 字段，最大值刷新时加锁 |
| `WindowMax` | 一个固定时间分段的最大值及发生时刻；按需进入新分段，不保存上一段或滚动历史 |
| `FixedHistogram` | 边界配置按契约共享；启用分布时由 owner 分配 long[]，每次仅定位桶并原子加一 |

请求、样本、字节使用同一速率实现，分别得到次/秒、样本/秒、字节/秒；收、发方向分开持有。
`RateMeter.mark(amount)` 支持批量增量，调用方应尽量按已有报文/批次记录，而非逐字节调用。
直接使用其 `getCount()` 暴露累计，避免再维护一份同口径计数。
多个线程竞争同一实例会增加原子更新成本；优先按运行 owner 持有，再在查询侧汇总，避免全节点写入同一个统计热点。
首次五秒节拍前的 EWMA 为零；1/5/15分钟是平滑时间常数，不是最近窗口的精确平均或历史曲线。
空闲读取会衰减，长时间空闲按公式一次推进，不随空闲时长循环。

```java
final RateMeter receivedBytes = new RateMeter();
final DurationSummary requestDuration = new DurationSummary();
final WindowMax hourlyDuration = new WindowMax(java.time.Duration.ofHours(1));

// 在通信完成边界，复用一次时钟读取；不创建逐请求统计对象。
long elapsedNanos = System.nanoTime() - startedAtNanos;
long completedAtMillis = System.currentTimeMillis();
receivedBytes.mark(readableBytes);
requestDuration.record(elapsedNanos, completedAtMillis);
hourlyDuration.record(elapsedNanos, completedAtMillis);
```

通信 RTT、排队等待和采集任务耗时需要分别选择埋点起止；不能将任务流耗时直接当 RTT，
也不能将最后收到原始数据当成最后有效输出。仅变化上报的订阅长期零流量可以正常。
耗时用单调时钟之差，发生时刻用 epoch 毫秒；错误、超时、取消由业务按口径独立计数。

各字段为弱一致观测，不是事务快照。`DurationSummary.snapshot()` 保证最大值/发生时刻成对，
其余累计仍是弱一致读取；`count == 0` 表示没有耗时样本。快照仅在请求侧分配。
业务接口继续使用 `@Counter` / `@Gauge` / `@Duration` / `@Timestamp` 等组合注解描述 getter，
单位、文案和国际化仍由 `MetadataUtils` / `MetricSet` 统一处理，算法不另建描述模型。

`WindowMax` 从 epoch 对齐固定时长，默认日窗口从 UTC 午夜开始；UTC+8 午夜可以显式传入
`new WindowMax(Duration.ofDays(1), Duration.ofHours(-8))`。固定24小时不能替代夏令时自然日。
窗口查询带 `available`，过期/未采样窗口不补真实零值；迟到旧窗口记录不会覆盖新窗口。
启用或实例重建之前的观测不会补齐，展示方需结合 owner 的观测起点/实例代次解释覆盖范围。
“本小时/今日”峰值可使用它；精确“最近60分钟”峰值需额外保存候选或分桶，不由 EWMA 提供。
吞吐峰值还须明确采样间隔，例如“一分钟平均字节速率峰值”，不能标为瞬时峰值。

直方图默认为可选，不给所有点位/连接预分配桶。边界、单位及观测起点相同的桶先相加再求分位，
不可平均 P95。`percentile` 返回所在桶上界并限制为实际最大值，无样本/无效参数为 NaN。
累计直方图不是最近一小时分布；`ConnectionMetricsSupport` 复用同一实现，保留原14桶及观测起点格式。
节点/通道/采集器按真实 owner 持有统计；汇总峰值必须对同一时刻的汇总值求峰，不能相加不同时间的峰值。

参考：[Dropwizard EWMA/窗口统计](https://www.dropwizard.io/projects/metrics/en/latest/manual/core/)、
[Prometheus 分布与聚合](https://prometheus.io/docs/practices/histograms/)、
[Google SRE 四个黄金信号](https://sre.google/sre-book/monitoring-distributed-systems/)。
