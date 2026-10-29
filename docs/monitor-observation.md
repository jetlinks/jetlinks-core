# Monitor 执行观测

在 `monitor.observation` 包提供业务一次包装、按订阅选择和有限元数据采集，版本为 `1.3.3-SNAPSHOT`。`Monitor` 保持原四类能力接口；平台动态配置、来源持久化和跨服务引用传递由原所属模块接入。

## 接入方式

普通操作只包装一次：

```java
return Observations.observeMono("device.refresh", () -> refresh(deviceId));
```

需要输入输出引用时，来源声明一个可复用的 `ObservationSpec<I, O>`，不改变业务 DTO 或返回类型。以下业务类型及提取方法为示意，提取函数只读取已持有的不可变引用，不查库：

```java
static final ObservationSpec<SelectedWindow, MetricResult> CALCULATE =
    ObservationSpec.<SelectedWindow, MetricResult>of("metric.calculate")
        .onInput(MetricMetadata::writeInput)
        .onOutput(MetricMetadata::writeOutput);

return Observations.observeMono(CALCULATE, window, calculator::calculate);
```

平台在已有响应式入口安装原 Recorder 和可选的稳定 RecorderProvider，业务方法无需传入它们。按订阅读取 Recorder.class，缺省为 noop；provider 读取可信 Context 和本地策略快照，未绑定时为 disabled。不查找 Monitor、全局对象或远程配置，不使用 ThreadLocal。框架已持有原 recorder 时仍可显式传入，优先于 Context：`Observations.observeMono(recorder, CALCULATE, window, calculator::calculate)`。

```java
return execute(message)
    .contextWrite(context -> context
        .put(Recorder.class, originalRecorder)
        .put(RecorderProvider.class, recorderProvider));
```

| 订阅上下文 | 行为 |
| --- | --- |
| 无 recorder/provider | 仅执行原业务，不调用策略、创建 action 或提取元数据 |
| 仅 recorder | 保留原记录，跳过详细提取 |
| 仅 provider | 可贡献附加接收端；没有有效接收端则仅执行业务 |
| recorder 与 provider | 保留原记录，按策略决定详细提取及附加接收端 |

| 选择 | 行为 |
| --- | --- |
| `RecorderSelection.disabled()` | 保留原 recorder，跳过详细提取 |
| `RecorderSelection.capture(Recorder.noop(), revision)` | 仅丰富原记录，装配方须确保原接收端有效 |
| `RecorderSelection.capture(extraRecorder, revision)` | 保留原接收端并附加记录，业务仍只执行一次 |

## 契约与实现入口

- `Monitor.java`：保持原有接口；`ProxyMonitor.java` 补齐原 recorder 的 action/record 延迟代理，不感知 provider 或执行观测。
- `observation/RecorderProvider.java`：唯一的记录选择 SPI；`RecordMetadata.java`：提取函数的受限类型化写入 SPI。`ObservationSpec` 与 `RecorderSelection` 是不可变配置，不新增注册中心或存储 SPI。
- `observation/Observations.java`：两个上下文入口和两个显式 Recorder 入口，共享同一观察实现；业务值、原错误、空结果、取消保持原样，不新增订阅、重试、技术 span 或后台队列。
- `recorder/Recorder.java` 的 `combine`：去重与扁平化，向原生 action 分发生命周期，保留各接收端自身 ID、记录类型及 child parentId。各非 fatal 回调故障独立隔离；Supplier 每次组合事件最多求值一次，全 noop 不求值。

`RecordMetadata` 不暴露生命周期。每个回调成功后才提交暂存的写入，输入和输出合计最多 64 次来源写入、顶层集合元素累计最多 64 项，保留 Key 的类型与合并语义。值须为不可变引用；具体引用类型、字符串/传输字节上限由来源及原 codec 约束，不反射整个 DTO。

框架保留 `observation.*`。`metadataStatus` 为 `notRequested/captured/failed/truncated`；后续提取成功不掩盖此前缺口。`captured` 只表示提取完成，业务正常完成也不等于事务已提交或设备效果已确认。无有效接收端时不运行提取，不承诺持久化。

新路径要求原或附加 recorder 的 action/child 工厂返回本次未启动的新实例。已有手工 `action/transform` 入口不自动启用 provider，也不改变旧语义。平台 `CommandSupportMonitor` 当前提前 start，接入前须在所属实现提供原生未启动的 Recorder，安装至 Context 或显式交给工具；不通过扩展 Monitor 适配。仅升级 core 不表示平台已接入，recorder/provider/活 action 不跨服务序列化。

## 禁用路径与性能

上下文入口只增加一个 deferContextual 边界。没有 recorder/provider 时只做本地查找并直接调用业务，不分配每次选择、组合器、action、Capture 或 MetadataBuffer，也不再嵌套 defer。Mono 包装和捕获 lambda 仍有组装开销，不承诺零成本。

仅原记录器开启时不创建详细提取状态；已知 disabled 选择复用常量。动态 provider 每次订阅仍须读取本地有效快照；选择了详细采集后才按需分配提取预算与输出回调。原记录器非 noop 时保留安全组合的故障隔离，不以牺牲业务信号安全省略包装。

`src/test/java/org/jetlinks/core/benchmark/ObservationsBenchmark.java` 使用已有 JMH/GCProfiler。Context 快照预先创建，参数为 0/8 个已有键，每次包含组装、订阅及同步 block，不包含存储/网络 I/O。本机 JDK 21、aarch64，2 forks，3 次 1 秒预热、3 次 1 秒测量、单线程、256 MiB 堆，结果如下（均值；每格为 ns/op、B/op）：

| 路径 | 0 个已有键 | 8 个已有键 |
| --- | --- | --- |
| Mono.defer 基线 | 14.41、120.02 | 82.90、624.09 |
| 无绑定，复用静态 spec | 17.16、144.02 | 99.22、648.10 |
| 无绑定，字符串入口 | 22.63、184.03 | 106.95、688.11 |
| 显式 noop，静态 spec | 17.05、152.02 | 83.43、656.10 |
| 仅 disabled provider | 29.11、240.04 | 115.70、720.11 |
| 仅原记录器 | 202.53、1376.21 | 293.34、1876.29 |
| 详细记录，两项元数据 | 293.03、2160.33 | 404.53、2608.41 |

空 Context 的静态 spec 入口较基线约增加 24 B，字符串入口约增加 64 B，来自组装对象；高频来源可复用静态 spec。带已有键的测量包含原 Context 安装成本，部分耗时置信区间较宽；不据此推断生产吞吐、尾延迟或持久化成本，不以计时断言替代单测。原始报告为 `target/observations-benchmark.json` 与 `target/observations-simple-benchmark.json`，字符串入口以相同参数单独补测。

运行测试后可使用 Surefire XML 中的 `java.class.path`（测试 classpath）复现全部基准：

```sh
java -cp '<测试 classpath>' org.openjdk.jmh.Main 'org.jetlinks.core.benchmark.ObservationsBenchmark.*' -f 2 -wi 3 -i 3 -w 1s -r 1s -t 1 -jvmArgs '-Xms256m -Xmx256m' -prof gc -rf json -rff target/observations-benchmark.json
```

## 验证

2026-10-29 上下文入口与空路径优化后重新编译成功，以下 55 项测试全部通过，0 失败、0 错误、0 跳过；`git diff --check` 通过。Monitor 源码与 1.3 基线一致，不新增观察/provider 方法。

```sh
mvn -o -Dtest=MonitorObservationTest,CompositeRecorderTest,RecorderTest,ActionRecordTest,DeviceTracerTest,TraceHolderTest,SpanDataInfoTest test
```

`src/test/java/org/jetlinks/core/monitor/MonitorObservationTest.java` 的 26 项场景测试实际执行两探头有效选样、均温计算、指标/AI 异步汇合及告警判断；连续验证计算 → 告警 → 控制/通知，通知失败保留已完成控制及各阶段引用。闭环、并发、重试与 cache 已使用免传 Recorder 入口；还验证空上下文、仅 provider、显式覆盖、异常 Context、取消、动态开关、原 trace 关联、元数据失败与预算边界。

`CompositeRecorderTest.java` 的 8 项测试验证原生类型/父子、单次业务订阅、Supplier、各生命周期故障隔离及 fatal 透传；其余 21 项复用原记录与追踪回归。场景使用内存业务计算和测试接收端，未连接真实网关、告警 Service、通知服务、持久存储或集群。当前独立工作树的父 POM 相对路径存在坐标提示，Maven 已从本地仓库解析正确父工程；未修改父版本或 Maven 缓存。
