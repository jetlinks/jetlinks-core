# Monitor 执行观测

在 `monitor.observation` 包提供业务一次包装、按订阅选择和有限元数据采集，版本为 `1.3.3-SNAPSHOT`。`Monitor` 保持原四类能力接口；平台动态配置、来源持久化和跨服务引用传递由原所属模块接入。

## 接入方式

普通操作只包装一次：

```java
return Observations.observeMono(monitor.recorder(), "device.refresh", () -> refresh(deviceId));
```

需要输入输出引用时，来源声明一个可复用的 `ObservationSpec<I, O>`，不改变业务 DTO 或返回类型。以下业务类型及提取方法为示意，提取函数只读取已持有的不可变引用，不查库：

```java
static final ObservationSpec<SelectedWindow, MetricResult> CALCULATE =
    ObservationSpec.<SelectedWindow, MetricResult>of("metric.calculate")
        .onInput(MetricMetadata::writeInput)
        .onOutput(MetricMetadata::writeOutput);

return Observations.observeMono(monitor.recorder(), CALCULATE, window, calculator::calculate);
```

平台在已有响应式入口安装稳定的 `RecorderProvider`，业务方法只传入原 recorder。provider 每次实际订阅读取可信 Context 和本地策略快照，返回 `RecorderSelection`；不阻塞、远程取配置或替换原 recorder。没有绑定时使用 disabled 选择。原 Context 的 Monitor 键不影响观察工具；普通本地作用域读取继续用 `ContextView.getOrDefault`，不新增同功能包装 API。

```java
return execute(message)
    .contextWrite(context -> context.put(RecorderProvider.class, recorderProvider));
```

| 选择 | 行为 |
| --- | --- |
| `RecorderSelection.disabled()` | 保留原 recorder，跳过详细提取 |
| `RecorderSelection.capture(Recorder.noop(), revision)` | 仅丰富原记录，装配方须确保原接收端有效 |
| `RecorderSelection.capture(extraRecorder, revision)` | 保留原接收端并附加记录，业务仍只执行一次 |

## 契约与实现入口

- `Monitor.java`：保持原有接口；`ProxyMonitor.java` 补齐原 recorder 的 action/record 延迟代理，不感知 provider 或执行观测。
- `observation/RecorderProvider.java`：唯一的记录选择 SPI；`RecordMetadata.java`：提取函数的受限类型化写入 SPI。`ObservationSpec` 与 `RecorderSelection` 是不可变配置，不新增注册中心或存储 SPI。
- `observation/Observations.java`：两个静态 `observeMono` 入口只消费 Recorder，每次订阅读取 provider 并新建 action；业务值、原错误、空结果、取消保持原样，不新增订阅、重试、技术 span 或后台队列。
- `recorder/Recorder.java` 的 `combine`：去重与扁平化，向原生 action 分发生命周期，保留各接收端自身 ID、记录类型及 child parentId。各非 fatal 回调故障独立隔离；Supplier 每次组合事件最多求值一次，全 noop 不求值。

`RecordMetadata` 不暴露生命周期。每个回调成功后才提交暂存的写入，输入和输出合计最多 64 次来源写入、顶层集合元素累计最多 64 项，保留 Key 的类型与合并语义。值须为不可变引用；具体引用类型、字符串/传输字节上限由来源及原 codec 约束，不反射整个 DTO。

框架保留 `observation.*`。`metadataStatus` 为 `notRequested/captured/failed/truncated`；后续提取成功不掩盖此前缺口。`captured` 只表示提取完成，业务正常完成也不等于事务已提交或设备效果已确认。无有效接收端时不运行提取，不承诺持久化。

新路径要求原或附加 recorder 的 action/child 工厂返回本次未启动的新实例。已有手工 `action/transform` 入口不自动启用 provider，也不改变旧语义。平台 `CommandSupportMonitor` 当前提前 start，接入新路径前须在所属实现提供原生未启动的 Recorder，再显式交给工具；不通过扩展 Monitor 或覆写观察入口适配。仅升级 core 不表示平台已接入，provider/活 action 不跨服务序列化。

## 验证

2026-10-29 边界收敛后重新编译成功，以下 48 项测试全部通过，0 失败、0 错误、0 跳过；`git diff --check` 通过。Monitor 源码与 1.3 基线一致，已核对实际字节码无新增观察/provider 方法，构建目录无旧配置包装或工具类残留。

```sh
mvn -o -Dtest=MonitorObservationTest,CompositeRecorderTest,RecorderTest,ActionRecordTest,DeviceTracerTest,TraceHolderTest,SpanDataInfoTest test
```

`src/test/java/org/jetlinks/core/monitor/MonitorObservationTest.java` 的 19 项场景测试实际执行两探头有效选样、均温计算、指标/AI 异步汇合及告警判断；连续验证计算 → 告警 → 控制/通知，通知失败保留已完成控制及各阶段引用。还覆盖并发主体隔离、重试、cache、动态开关、原 trace 关联、元数据失败与预算边界。

`CompositeRecorderTest.java` 的 8 项测试验证原生类型/父子、单次业务订阅、Supplier、各生命周期故障隔离及 fatal 透传；其余 21 项复用原记录与追踪回归。场景使用内存业务计算和测试接收端，未连接真实网关、告警 Service、通知服务、持久存储或集群。当前独立工作树的父 POM 相对路径存在坐标提示，Maven 已从本地仓库解析正确父工程；未修改父版本或 Maven 缓存。
