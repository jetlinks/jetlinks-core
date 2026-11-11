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

注册表、采集累计、JMX、外部导出与前端由后续所属模块实现。现有 Recorder / ObservationContext 执行观测保持原契约；本能力没有采集 I/O、后台线程、新增 Trace 或平台 MBean。
