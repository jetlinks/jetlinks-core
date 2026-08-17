# DistinctDurationFlux 优化与压力测试计划

状态：PR #97 已更新；生命周期补充优化和 500 万 key 极限评测已完成并提交。

## 背景与当前结论

`org.jetlinks.core.utils.DistinctDurationFlux` 当前为每个订阅创建一个
`ConcurrentHashMap`，并在 `Schedulers.parallel()` 上注册一个约为
`duration * 1.1` 周期的清理任务。`FluxUtils.distinct(...)` 是上层稳定入口。

当前实现存在以下需要一并解决的问题：

1. key 过期后仅放行，不会更新保存的时间戳。在下一次周期清理前，同 key
   可能被连续放行，窗口语义不完整。
2. 无数据或少量数据的 distinct 仍会创建周期任务；大量独立 distinct 时，
   调度对象和任务队列的内存、订阅/取消成本可能大于数据处理成本。
3. 自定义 Subscriber 重复实现背压、取消、异常和 discard 处理，无法完整复用
   Reactor `Flux.distinct(keySelector, storeSupplier, predicate, cleanup)` 已提供的
   fusion、ConditionalSubscriber 和生命周期行为。
4. `System.currentTimeMillis()`、毫秒取整和周期值 `int` 强转存在时钟回拨、
   亚毫秒精度丢失和超长 Duration 溢出风险。

## 目标

1. 使用 Reactor 官方 `Flux.distinct(...)` 承担订阅、背压、fusion、discard、
   终止和取消语义，自定义部分只负责固定时间窗口 store。
2. 保持 `FluxUtils.distinct(Function, Duration)` 公共 API 和每订阅独立状态。
3. 修复固定窗口语义：首次通过后，在 TTL 内抑制重复；TTL 后首次重新通过并
   立即建立新窗口，后续重复继续被抑制。重复命中不延长窗口。
4. 保持 null key 直接通过的既有行为。
5. 消除“每个 distinct 一个周期任务”的线性调度开销。
6. 同时改善或至少不恶化吞吐、尾延迟、对象分配和保留内存，重点覆盖大量
   distinct、单个 distinct 无数据/少量数据以及高并发 distinct。

## 影响范围与 owning module

- owning repo：`jetlinks-core`
- 生产入口：
  - `src/main/java/org/jetlinks/core/utils/DistinctDurationFlux.java`
  - `src/main/java/org/jetlinks/core/utils/FluxUtils.java`
- 功能测试：
  - `src/test/java/org/jetlinks/core/utils/DistinctDurationFluxTest.java`
  - `src/test/java/org/jetlinks/core/utils/FluxUtilsTest.java`
- 基准与压力测试：
  - 新增 `src/test/java/org/jetlinks/core/benchmark/DistinctDurationFluxBenchmark.java`
  - 新增 `src/test/java/org/jetlinks/core/benchmark/DistinctDurationFluxMemoryStress.java`

不需要新模块，不修改 cloud.jetlinks 中的各业务调用点。

## 非目标

1. 不改变 fixed-window 为 debounce、sliding window 或重复命中续期。
2. 不引入跨订阅共享去重状态、分布式去重或持久化去重。
3. 不在热路径增加 TraceHolder、Micrometer 或全局静态统计计数。
4. 不为基准测试改变业务语义，也不使用并发调用同一个 Subscriber 的非法
   Reactive Streams 测试方式。
5. 不直接删除公开的 `DistinctDurationFlux` 类型；先保持二进制和源码兼容。

## 必须冻结的兼容语义

1. store 由 supplier 按订阅创建，不同订阅互不影响。
2. null key 不进入 store，每次直接通过。
3. 同 key 在窗口内只通过第一次；拒绝项补偿下游 demand。
4. TTL 后第一次通过时必须写入新的 deadline，不能等待后台清理决定语义。
5. complete、error、cancel 都释放 store；cleanup 必须幂等。
6. keySelector 或 predicate 异常走 Reactor 标准 operator error/discard 路径。
7. Duration 的零值、负值、亚毫秒值和极大值需要显式定义并测试，不能依赖
   scheduler 的隐式报错或数值溢出。
8. 使用单调时钟计算经过时间，测试通过可注入 ticker 推进时间，不依赖 sleep。

## 推荐方案与候选验证

### 操作符边界

保留 `DistinctDurationFlux` 公共类型和 `create(...)` 入口，但内部委托给：

```java
source.distinct(
    nullableKeySelector,
    durationStoreSupplier,
    durationStorePredicate,
    durationStoreCleanup
)
```

官方操作符负责 Reactive Streams 协议，自定义 store 只实现同步、每订阅的
固定窗口判定。该层是纯同步过滤，不新增 tracing。

### 最终方案：分层紧凑状态 + 到期队列

最终 store 使用单调时钟，并按数据量分层：

1. 0 个 key：store 只有一个惰性 state 引用，不创建数据状态。
2. 1 个活跃 key：惰性创建单 key + timestamp 状态。
3. 2～8 个活跃 key：使用 `Object[] + long[]` 紧凑数组，避免小流创建 Map 节点。
4. 超过 8 个活跃 key：升级为链式哈希桶；同一个 `LargeEntry` 同时承担哈希节点
   和 FIFO 到期队列节点，避免 Map node、boxed timestamp 和 expiry node 三份对象。
5. 每次判定先从队首摊销清理到期项；清理后自动降级回小数组或单 key 状态。

预期单条判定和清理均为摊销 O(1)，内存为 O(TTL 内通过的不同 key 数量)，
不再创建每订阅周期任务。空闲流中到期状态是否需要立即物理释放，由下面的
保留堆实测决定；语义上下一条数据必须把到期 key 视为不存在。

实测曾淘汰两个中间候选：`HashMap + Expiry` 的全唯一分配量比 baseline 高约
14%；单块开放寻址数组虽然把分配降到约 90.7 MB/op，但大 `long[]` 成为 G1
humongous allocation，使全唯一吞吐下降约 23%。最终链式 Entry 同时满足吞吐、
分配和保留内存要求。

### 对照候选

1. Reactor distinct + ConcurrentHashMap：用于隔离“官方操作符替代”本身的
   开销，不能作为最终 TTL 清理方案。
2. Reactor distinct + 普通 HashMap/惰性同-key 清理：吞吐对照；持续高基数下
   会保留无后续访问的陈旧 key，不作为最终方案。
3. Caffeine expireAfterWrite：当紧凑 store 的空闲保留内存不达标时再评估。
   不使用 expireAfterAccess；`maximumSize` 会提前淘汰并改变严格去重语义，
   除非后续单独确认降级策略，否则不启用。

候选只保留最终胜出实现；被拒绝的 benchmark-only 实现不进入生产代码。

## 功能测试目标

使用 StepVerifier 和可控 ticker，不使用 Thread.sleep：

1. 空 Flux 正常完成，不创建数据状态。
2. null key 全部通过，保持现有调用兼容。
3. 同 key 在 TTL 内仅通过第一次。
4. TTL 后第一次重新通过，紧邻的下一条再次被抑制，专门覆盖当前时间戳未更新缺陷。
5. 多 key 各自维护窗口，到期顺序不同也能正确清理。
6. 两次订阅状态隔离；retry/repeat 重新订阅时获得新 store。
7. complete、error、cancel 后状态可释放，cleanup 重复调用安全。
8. keySelector 异常、discard hook、有限 demand 和拒绝后的 request 补偿正确。
9. Fuseable 与 `.hide()` 非 Fuseable 源输出一致。
10. Duration 零值、负值、亚毫秒和超大值按冻结规则处理。
11. 高基数持续输入并推进 ticker 后，store 大小保持在 TTL 窗口边界，不随
    历史总 key 数无限增长。

## 压力测试方法

### 基准纪律

1. 先只新增测试/benchmark，在旧实现上生成 baseline；再改生产实现，用完全
   相同的源码、JVM、参数和机器生成 optimized 结果。
2. 使用现有 JMH 1.35 基础设施，正式结果至少 2 forks、3 次 warmup、5 次
   measurement；快速 smoke 与正式报告分开。
3. 固定 JVM 堆、GC 和 Reactor 版本，记录 Java 版本、CPU、OS、git commit、
   JVM 参数及 benchmark 参数。
4. raw JMH/JFR/histogram 输出写入 `target/distinct-duration-benchmark/`，不提交；
   设计文档只回填汇总结论和复现命令。

### 指标

吞吐和延迟：

- ops/s、items/s
- average time
- sample time 的 p50、p95、p99
- 1/4/8/16 线程扩展效率

分配与 GC：

- JMH GCProfiler：`gc.alloc.rate`、`gc.alloc.rate.norm`、GC count/time
- JFR：对象分配热点、GC pause、线程和调度热点

保留内存：

- 强制 GC 后 heap used 和 class histogram
- RSS
- Native Memory Tracking summary
- 按关键类统计对象数量/字节：旧 Subscriber、ConcurrentHashMap 节点、Long、
  Reactor distinct Subscriber、新 store/expiry、scheduler periodic task
- 计算 bytes/distinct、bytes/active-key 和取消后的回收比例

### 场景一：单个 distinct 的数据热路径

| 维度 | 参数 |
|---|---|
| source | Fuseable `Flux.range`；非 Fuseable `.hide()` |
| 每次数据量 | 1,000,000 items |
| key 分布 | 单一重复 key；64/4096 热 key；90% 热点+10%新 key；全部唯一 |
| TTL | 1s、30s；基准内不依赖后台清理决定正确性 |
| 线程 | 1，另在高并发场景扩展 |

目的：隔离每条 onNext、重复拒绝、唯一 key 插入、fusion 和 request 补偿成本。

### 场景二：大量 distinct，单个无数据或少量数据

| 维度 | 参数 |
|---|---|
| active distinct 数 | 1,000；10,000；50,000；内存允许时追加 100,000 |
| 每个 distinct 数据量 | 0；1；8 |
| 生命周期 | 立即完成；订阅后 idle 保持活跃；主动 cancel |
| 数据形态 | 无 key；单 key；少量相同 key；少量不同 key |
| TTL | 1s、30s |

对每组记录：

1. 批量创建/订阅/取消耗时与分配字节。
2. active steady-state 强制 GC 后的 heap、RSS、对象直方图。
3. 超过 TTL 且仍 idle 时的保留堆。
4. complete/cancel 后再次强制 GC 的残留和回收比例。
5. scheduler 周期任务数量是否随 distinct 数线性增长。

内存压力进程使用 `Flux.never()` 表示无数据活跃流，使用
`Flux.concat(smallData, Flux.never())` 表示少数据后 idle；通过明确的 phase
barrier 暴露 READY 状态后再执行 jcmd/JFR 采样，不使用随机 sleep。

### 场景三：高并发 distinct

高并发定义为多个独立 subscription 并行处理，保证每个 subscription 的
onNext 串行，不对同一个 Subscriber 进行并发调用。

| 维度 | 参数 |
|---|---|
| JMH threads | 1；4；8；16，最高不超过机器逻辑核数的 2 倍 |
| 每线程活跃 distinct | 1；64；1024 |
| 每 distinct 数据量 | 1；8；持续批次 |
| key 分布 | 高重复；高基数；90/10 混合 |
| 生命周期 | 高频短订阅；长期活跃并周期输入；批量 cancel |

重点比较总吞吐、扩展效率、p99、分配率、GC pause、订阅/取消竞争和公共
scheduler 压力。异步 source 的生产协调成本单独标识，避免把 sink/线程池开销
误算为 distinct 算法差距。

## 保留内存采样流程

`DistinctDurationFluxMemoryStress` 作为独立 forked JVM 运行，并提供明确阶段：

1. `BASELINE`：未创建 distinct，采集初始 heap/RSS。
2. `ACTIVE_EMPTY`：创建 N 个无数据活跃 distinct 后采集。
3. `ACTIVE_SMALL`：每个流输入 1 或 8 条后保持 idle 并采集。
4. `POST_TTL_IDLE`：超过 TTL、不再输入但不取消时采集。
5. `POST_CANCEL`：统一取消/完成、强制 GC 后采集。

统一使用：

```text
-Xms2g -Xmx2g -XX:+UseG1GC -XX:NativeMemoryTracking=summary
```

采样工具：

```text
jcmd <pid> GC.run
jcmd <pid> GC.heap_info
jcmd <pid> GC.class_histogram
jcmd <pid> VM.native_memory summary
```

同时保留 JFR，用于解释 heap 差异来自 store、key、到期记录还是 scheduler task。

## 初始验收标准

正确性门槛：

1. 所有冻结语义和功能测试通过。
2. 不再为每个 distinct 创建周期清理任务。
3. 无数据、过期重建窗口、cancel/error/complete 无泄漏。

性能门槛：

1. 非 Fuseable 的高重复和全唯一主场景吞吐不得比 baseline 下降超过 5%。
2. p99 不得下降超过 10%；若平均吞吐提升但尾延迟退化，不能只按吞吐通过。
3. 1/4/8/16 线程扩展效率不得劣于 baseline。
4. 高频短订阅和大量 idle distinct 应有明确改善，而不是仅优化单流大数据。

内存门槛：

1. 50,000 个无数据活跃 distinct 的 retained bytes/distinct 目标至少降低 50%。
2. 每流 1/8 条数据时 retained bytes/distinct 不得高于 baseline，目标降低 20%。
3. 分配字节/订阅和重复项分配字节不得高于 baseline。
4. cancel/complete 后强制 GC，新增 live heap 应回收到创建前基线的 5% 误差内。
5. `POST_TTL_IDLE` 若比 baseline 保留更多 key/expiry，首选候选不得直接交付；
   必须调整紧凑状态/清理策略或重新评估 Caffeine 候选。

上述阈值先作为决策门槛；如果 baseline 证明某项受 JVM 噪声影响无法稳定测量，
需先回写测量方法和新阈值并重新确认，不能在结果出来后静默放宽。

## 实施步骤

1. 补齐功能回归测试和 JMH/内存压力工具，不修改生产实现。
2. 在旧实现上运行一次 smoke，确认基准有效；随后运行正式 baseline 并保存原始结果。
3. 在 benchmark 范围比较紧凑 store、CHM 对照和 Caffeine 对照，结合吞吐、
   分配、保留堆选择最终 store。
4. 使用 `Flux.distinct(...)` 重构 `DistinctDurationFlux`，保持公共类型、入口、
   null key 和订阅隔离兼容；补充生命周期/惰性清理必要注释。
5. 集中运行 DistinctDurationFlux/FluxUtils 单元测试和模块相关测试。
6. 使用与 baseline 完全相同的参数运行 optimized JMH、JFR 和保留堆压力测试。
7. 对比全部场景和验收阈值；若候选失败，回到第 3 步，不通过特调测试数据掩盖。
8. 在本文档回填最终方案、测试命令、汇总结果和剩余风险；原始报告仍保留在 target。

## 验证阶段划分

遵循阶段性验证，不在每个小编辑后重复完整构建：

1. 基准设施阶段：test-compile + benchmark smoke。
2. 实现阶段：定向 DistinctDurationFluxTest、FluxUtilsTest。
3. 收口阶段：jetlinks-core 模块测试 + 正式 JMH/JFR/内存压力测试。

## 风险与待确认点

1. 当前目标仓库为 `/Users/zhouhao/IdeaProjects/jetlinks/jetlinks-core`，当前分支为
   `1.3`；若“dev 下”指另一个 worktree/分支，需要在实施前切换落点。
2. `DistinctDurationFlux` 是公开类型，不能只修改 `FluxUtils` 后直接删除该类。
3. 惰性清理在 idle 时可能保留已到期 key；本计划把它作为必须实测且不得回退
   的内存门槛，不提前用主观判断接受。
4. 高并发测试必须保持 Reactive Streams 串行信号契约，否则结果无效。
5. 本能力是通用同步操作符，没有集中式长期管理对象；生产 MBean 会引入全局状态
   并污染热路径，因此不新增 MBean。JMH/JFR/直方图承担本次性能可观测性。

## 最终结果回填

### 实现落点

- `src/main/java/org/jetlinks/core/utils/DistinctDurationFlux.java`
  - 保留公开类型、受保护构造器与 `create(...)` API。
  - 内部委托 Reactor 四参数 `Flux.distinct(...)` 处理背压、fusion、discard、异常
    和 complete/error/cancel cleanup。
  - null key 使用私有 sentinel 绕过 store，保持每次直接通过。
  - 使用 `System.nanoTime()`；零/负 Duration 明确拒绝，纳秒 Duration 保留精度，
    超出 long nanos 的正 Duration 饱和为 `Long.MAX_VALUE`。
  - 使用上述 0/1/小数组/链式哈希分层 store；重复命中不续期。
- `src/test/java/org/jetlinks/core/utils/DistinctDurationFluxTest.java`
  - 使用可控 ticker，无 sleep，覆盖 fixed-window、过期重建、多 key、碰撞、
    高基数清理、null、订阅隔离、backpressure/discard、异常、fusion 和 Duration。
- `src/test/java/org/jetlinks/core/benchmark/DistinctDurationFluxBenchmark.java`
  - 覆盖 1,000,000 items、四种 key 分布、Fuseable/非 Fuseable、生命周期和
    1/4/8 线程 JMH；使用 GCProfiler，正式模式可附加 JFR。
- `src/test/java/org/jetlinks/core/benchmark/DistinctDurationFluxMemoryStress.java`
  - 独立 JVM 采集 heap、RSS、NMT、class histogram，支持 raw subscription
    负对照，覆盖 10,000/50,000 distinct 与 0/1/8 条数据。

### 功能与模块测试

- `mvn -Dtest=DistinctDurationFluxTest,FluxUtilsTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - 20 tests，0 failure，0 error。
- JaCoCo（`DistinctDurationFlux.java`）：186/211 line、83/96 branch，约
  88.2% line / 86.5% branch。
- `mvn test`
  - 共执行 592 tests，本次相关测试全部通过；全量结果为 0 failure、11 error。
  - 11 个 error 全部来自既有 `ByteLayoutImplTest` 的静态布局为 null；随后单独执行
    `mvn -Dtest=ByteLayoutImplTest test` 为 16/16 通过，说明是全量测试顺序/隔离
    问题，不在本次变更范围。

### 单线程 JMH quick 对比

环境：JDK 21.0.10、G1、`-Xms2g -Xmx2g`、1 fork、1×1s warmup、2×1s
measurement；每个 data op 处理 1,000,000 items。原始 JSON 位于
`target/distinct-duration-benchmark/`。

非 Fuseable 主结果：

| key 分布 | throughput baseline → final | 变化 | alloc/op baseline → final | 变化 | p99 baseline → final |
|---|---:|---:|---:|---:|---:|
| REPEAT | 38.81 → 62.58 ops/s | +61.2% | 40.00 → 16.00 MB | -60.0% | 28.80 → 17.97 ms |
| HOT_KEYS | 35.50 → 48.32 ops/s | +36.1% | 55.69 → 31.69 MB | -43.1% | 32.60 → 23.95 ms |
| MIXED | 23.78 → 29.87 ops/s | +25.6% | 48.36 → 22.90 MB | -52.6% | 52.43 → 41.55 ms |
| UNIQUE | 15.02 → 25.15 ops/s | +67.5% | 104.78 → 80.79 MB | -22.9% | 86.64 → 85.46 ms |

Fuseable 四种分布吞吐提升约 36.6%～71.9%，分配趋势一致；所有单线程 p99
均未退化。

### 大量短生命周期 distinct

每个 JMH operation 创建并完成 1,000 个 distinct：

| 单流数据量 | throughput baseline → final | 倍数 | alloc/op baseline → final |
|---:|---:|---:|---:|
| 0 | 430 → 27,313 ops/s | 63.5x | 398 → 296 KB |
| 1 | 421 → 16,091 ops/s | 38.2x | 591 → 376 KB |
| 8 | 387 → 5,021 ops/s | 13.0x | 1,000 → 544 KB |

### 保留内存

独立 JVM、强制 GC 后的 50,000 distinct active heap：

| 单流数据量 | baseline | final | 总量变化 | raw subscription | distinct 净开销 baseline → final |
|---:|---:|---:|---:|---:|---:|
| 0 | 258 B/实例 | 141 B/实例 | -45.3% | 45 B/实例 | 213 → 96 B（-54.9%） |
| 1 | 450 B/实例 | 222 B/实例 | -50.7% | 101 B/实例 | 349 → 121 B（-65.3%） |
| 8 | 842 B/实例 | 350 B/实例 | -58.4% | 101 B/实例 | 741 → 249 B（-66.4%） |

- final 的 post-TTL-idle 与 active heap 基本一致，分别为 141/222/350 B/实例，
  均低于旧实现 post-TTL 的 447/396/458 B/实例。惰性状态不会由后台任务主动清空，
  但总保留堆仍更低，下一条信号会摊销清理并保证语义正确。
- cancel 后新增 live heap 约 6 B/实例，处于 baseline 噪声范围。
- class histogram：旧实现 50,000 个空 distinct 对应 50,000
  `ScheduledFutureTask` + 50,000 `PeriodicSchedulerTask`；final 均为 0。
- 50,000 空流 RSS 增量约从 43.4 MB 降到 12.2 MB（-72%）；8 条从约
  102.9 MB 降到 81.5 MB（-21%）。1 条场景 RSS 增量约 67.2 → 69.8 MB，
  在固定 `-Xms2g` 的 G1 resident/commit 噪声下未随 live heap 同比例下降，已保留
  NMT 与 histogram 原始证据，不以 RSS 单项替代 live heap 判断。

### 高并发 JMH quick

多个 JMH worker 各自使用独立 subscription，未并发调用同一个 Subscriber。
非 Fuseable 的四种 key 分布：

| threads | throughput 变化范围 | p99 变化范围 |
|---:|---:|---:|
| 4 | +10.2%～+132.7% | -52.7%～-65.5% |
| 8 | +56.9%～+121.4% | -36.5%～-45.0% |

quick 结果只用于同机前后回归，不声明为跨机器容量结论；正式容量评估仍可使用
benchmark 的非 quick 模式（2 forks、3 warmup、5 measurement、JFR）复现。

### 复现命令

```bash
mvn -DskipTests test-compile
mvn -DincludeScope=test dependency:build-classpath -Dmdep.outputFile=target/test-classpath.txt
java -Xms2g -Xmx2g -XX:+UseG1GC \
  -cp "target/test-classes:target/classes:$(<target/test-classpath.txt)" \
  org.jetlinks.core.benchmark.DistinctDurationFluxBenchmark data result-name 1 true
java -Xms2g -Xmx2g -XX:+UseG1GC -XX:NativeMemoryTracking=summary \
  -cp "target/test-classes:target/classes:$(<target/test-classpath.txt)" \
  org.jetlinks.core.benchmark.DistinctDurationFluxMemoryStress 50000 8 1000 result-name
```

- 实现 commit：`04a5f4ca004e821b6326678a2bb0242e63f81bfb`。
- Pull Request：https://github.com/jetlinks/jetlinks-core/pull/92

## LargeState 二阶段通用性能优化

设备完整链路的早期 structured JFR 显示，当前通用实现仍有可操作热点：
`LargeState.find` 约占 9.45%，`removeFromTable` 约占 6.97%，`DurationStore.add`
和 `LargeState.add` 合计约占 2.9%。该 profile 早于 Components 最终 D4，只用于确定
Core 候选；本阶段必须以当前 Core HEAD 的独立 JMH/JFR 为归因基线。

### 目标与边界

本阶段仅优化 `DistinctDurationFlux.LargeState` 的通用链式哈希实现，不修改
`FluxUtils.distinct`、公开构造/API、Reactor 四参数 `Flux.distinct` 边界、fixed-window、
null、backpressure、fusion、discard 和 cleanup 语义。设备 UID 提取、String 专用开放
寻址表、TopicPayload 缓存、EventBus 及集群逻辑不进入 Core。

继续保持每订阅局部、Reactive Streams 串行的非并发状态，不新增 scheduler、线程池、
ThreadLocal、MBean 或逐消息 tracing。该同步工具没有新的异步边界；JMH、JFR 和内存压力
进程继续承担性能可观测性。

### 推荐实现

1. 将 `contains` 后再 `add` 合并为一次 `addIfAbsent`：每条 key 只计算一次 spread hash、
   只遍历一次 bucket，未命中时直接插入；
2. bucket 链改为首次写入顺序。全局 expiry FIFO 与每个 bucket 的头部保持相同的最老
   entry，因此到期删除只移动 bucket head，不再为每个过期 key 重新遍历 bucket；resize
   必须恢复 bucket 内的时间顺序，不能增加长期 tail 数组或逐 entry 前驱引用；
3. LargeState 清理后只剩一个 entry 时，直接读取 `firstExpiry` 的 key/timestamp，避免
   通过 hash table 再查询一次；
4. 对 8/9 个活跃 key 的 Large/Small 转换增加滞后或等价防抖，避免到期和新写入交替时
   反复重建数组与 Entry。具体阈值只在边界 churn 基准证明收益且不增加小状态内存后保留。

不预设缓存 hash 到 `LargeEntry`：这可能扩大每个活跃 key 的对象尺寸；只有 JOL/loaded
heap 证明没有对象尺寸增长且 JFR 仍指向 hash 计算时才评估。也不重新采用单块开放寻址数组，
此前全唯一场景已出现 G1 humongous allocation 和约 23% 吞吐回退。

### 基准与验收

先在当前实现生成 baseline，再改生产代码。除既有 REPEAT/HOT_KEYS/MIXED/UNIQUE、
Fuseable/非 Fuseable 和 1/4/8 worker 外，补充以下定向场景：

1. 所有 key 使用相同 hash 的碰撞链，覆盖重复查询和持续到期删除；
2. 活跃 key 长期维持在 8/9 附近的升降级 churn；
3. 625、2,500、25,000 个活跃 key 的 fixed-window 稳态到期/插入，而不是只测一次性
   1,000,000 个唯一 key；
4. resize 前后按不同 bucket/相同 bucket 到期，验证 bucket head 与 expiry FIFO 一致；
5. retained bytes/active key、resize allocation、GC pause 和 cancel 后回收。

正式结果至少 3 fork、2 次预热、5 次 measurement，baseline/optimized 交错执行并使用
`-prof gc`；主结果报告 ns/item、items/s、B/item、p50/p95/p99。保留门禁为：

1. 所有冻结语义、碰撞、背压、fusion、discard 和 lifecycle 测试通过；
2. 稳态到期和碰撞主场景 CPU 至少改善 5%，全唯一、高重复和短生命周期场景不得回退
   超过 5%；
3. p99 不得回退超过 5%，分配和 retained bytes/active key 不得增加；
4. 若只有设备特定数据分布改善、通用矩阵未达门禁，则撤回 Core 结构变化，把优化留在
   Components 专用实现。

### 实施与复审结果

本阶段已按上述边界完成，生产代码仍只修改
`src/main/java/org/jetlinks/core/utils/DistinctDurationFlux.java`：

1. `LargeState.contains(...) + add(...)` 已合并为单次 `addIfAbsent(...)`，每次写入只计算
   一次 spread hash，并在一次 bucket 遍历中同时完成判重和 tail 定位；
2. bucket 改为按写入时间链接，到期项通常就是 bucket head；resize 使用 low/high split，
   保持每个新 bucket 的相对写入顺序，因此不会破坏 expiry FIFO 与 bucket head 的一致性；
3. LargeState 只剩一个 entry 时直接读取 `firstExpiry`，不再进行二次 hash 查询；
4. 8/9 key 边界采用防抖策略：到期后剩 8 个 key 且当前是新 key 时继续保留 LargeState，
   避免每条数据重建状态；只有边界重复 key 才压缩回 SmallState；
5. 判等先检查 identity，再调用 `equals`。`LargeEntry` 未增加 hash、前驱或 tail 字段，
   每个活跃 key 的常驻对象布局不变；未新增 scheduler、线程池、ThreadLocal、MBean、
   Trace 或响应式异步边界。

复审重点均已通过确定性模型验证：bucket 写入顺序、resize low/high split、expiry head 删除、
8/9 key 升降级，以及普通 hash/全碰撞 hash 下的 fixed-window 行为一致。

### 正确性验证

- 定向测试：
  `mvn -Dtest=DistinctDurationFluxTest,FluxUtilsTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - 25 tests，0 failure，0 error；
  - 包含全 hash collision 跨 resize/持续到期、8/9 key 长期 churn、边界重复后再升级；
  - collision 与 spread 两种 hash 分布各执行 20,000 次确定性随机 churn，并逐次与
    `LinkedHashMap` fixed-window 参考模型比对。
- Core 全量：`mvn test`
  - 637 tests，0 failure，0 error，1 skipped。
- `git diff --check`：通过。

### 正式性能对比

环境：JDK 21.0.10、G1、`-Xms2g -Xmx2g`、单线程。基准提交为 `58dadb97`；正式
collision/JFR 矩阵使用 3 forks、2 次预热、5 次测量。JFR 改为基准第四参数显式启用，
避免默认录制干扰亚微秒延迟。

AverageTime 主结果（baseline → optimized）：

| 活跃 key | hash 分布 | ns/op | 改善 | B/op |
|---:|---|---:|---:|---:|
| 9 | spread | 158.570 → 23.787 | 85.0% | 552.35 → 32.05 |
| 9 | collision | 165.930 → 33.045 | 80.1% | 552.36 → 32.07 |
| 625 | collision | 2,502.507 → 1,271.489 | 49.2% | 37.32 → 34.69 |
| 2,500 | collision | 10,648.961 → 5,161.025 | 51.5% | 54.27 → 43.04 |
| 25,000 | collision | 124,773.660 → 56,613.584 | 54.6% | 295.15 → 152.07 |

9-key 分配减少约 94.2%，来自消除 Small/Large 边界反复重建。碰撞链越长，单次判重
与到期删除合并带来的收益越明显。

普通 spread hash 另使用无 JFR、3 forks、3 次预热、7 次测量复测 AverageTime，避免
把录制扰动当作实现差异：

| 活跃 key | baseline → optimized | 改善 | B/op |
|---:|---:|---:|---:|
| 625 | 29.440 → 28.441 ns/op | 3.4% | 32.001 → 32.001 |
| 2,500 | 29.826 → 28.742 ns/op | 3.6% | 32.001 → 32.001 |
| 25,000 | 29.750 → 28.068 ns/op | 5.7% | 32.001 → 32.001 |

无 JFR、5 forks 的 SampleTime 尾延迟复测：

| 活跃 key | mean ns/op | p95 | p99 |
|---:|---:|---:|---:|
| 625 | 72.788 → 73.160（+0.5%） | 87 → 84 | 115 → 111 |
| 2,500 | 74.615 → 74.300（-0.4%） | 89 → 85 | 116 → 113 |
| 25,000 | 73.491 → 75.169（+2.3%） | 85 → 84 | 120 → 123（+2.5%） |

普通 hash 的最大 p99 回退为 2.5%，低于 5% 门禁，B/op 不变。JFR 的 25,000-key
collision profile 中，baseline 的 `removeFromTable`、`drainExpired`、`find` 分别约占
33.97%、15.91%、10.72%；optimized 中 `removeFromTable/find` 不再是热点，剩余主要是
不可避免的 collision `addIfAbsent`/`equals` 遍历。

原始结果保留在以下构建目录，不提交：

- baseline：
  `target/distinct-duration-benchmark/core-large-churn-baseline-formal-t1.json`
- optimized：
  `target/distinct-duration-benchmark/core-large-churn-optimized-formal-t1.json`
- 无 JFR spread AverageTime：
  `target/distinct-duration-benchmark/core-large-churn-spread-average-*-t1.json`
- 无 JFR spread SampleTime：
  `target/distinct-duration-benchmark/core-large-churn-spread-latency-*-t1.json`

结论：Core 通用优化满足语义、吞吐、尾延迟和分配门禁，可以保留；Components 的设备
专用链路优化仍应在此通用实现之上单独评估，不向 Core 引入设备 Topic 或 UID 特化。

- 本阶段实现 commit：`794fe8189800919621e96553d22d38281d3988f6`。
- Pull Request：https://github.com/jetlinks/jetlinks-core/pull/97

## LargeState 生命周期优化

PR #97 在最新 `1.3`（已包含 #96、#98）复审时确认，稳态 churn 基准未覆盖以下
状态变化：

1. LargeState 曾扩容到高基数，TTL 内活跃 key 随后长期回落到低基数；
2. 高基数窗口整体空闲超过 TTL，下一条消息恢复投递。

当前 table 只扩容不缩容，8/9 key 防抖会继续保留历史峰值容量；整个窗口过期时仍逐
entry 计算 hash 并从 bucket 删除。设备消息订阅按订阅持有独立 store，这两种情况会分别
放大常驻内存和恢复首条消息的尾延迟。

本阶段保持公开 API、fixed-window、null、背压、fusion、discard、cleanup 和每订阅串行
状态不变，只补充以下内部策略：

1. 当最新 entry 也已过期时直接清空 LargeState，避免全窗口逐项删除；
2. table 严重低载且 LargeState 仍需保留时按 25% 阈值逐级缩容；缩容完成时已有 entry
   占用 25%～50%，并设置最小容量 16，避免重新引入 8/9 key 每条重建；
3. 缩容按 expiry FIFO 重建 bucket 顺序，继续保持到期项通常位于 bucket head；
4. 增加 25,000 -> 8/625 key、全窗口过期和碰撞缩容的确定性测试，并为
   `idleResume`、`cardinalityDrop` 增加独立 JMH 场景。

验收要求：fixed-window 参考模型和 Core 全量测试通过；25,000 key 全过期恢复不再逐 key
计算 hash；25,000 -> 9 key 后 table 回到最小容量；原有 9/625/2,500/25,000 key 稳态
吞吐和分配不得出现超过 5% 的稳定回退。

### 实施与最终验证

生产实现继续只修改 `DistinctDurationFlux.LargeState`，没有改变 Reactor 操作符边界：

1. 检测到队首过期后，仅在第二个 entry 也过期时检查队尾；队尾已过期则 O(1) 清空整个
   table 和 expiry queue，不再逐 key 计算 hash 和删除 bucket；
2. 部分过期后在下一次写入前检查 table 负载，低于 25% 时按 2 的幂逐级缩容；重建沿
   expiry FIFO 遍历并按 bucket 维护 tail，保留 bucket 内写入顺序；
3. 普通写入增加空 bucket 直接插入快路径，非空 bucket 才执行 identity/equals 判重；
4. `drainExpired` 只保留队首到期判断，实际删除拆到 `drainExpiredEntries`。编译日志证明
   早期 25,000-key spread 的快慢双峰来自该方法不同 C2 分支画像；拆分后 5 个 fork
   收敛，不再出现约 17.8/24.4 ns/op 双峰。

`LargeState` 的数量不变量保持成立：它只会在一次 `DurationStore.add(...)` 结束时保留
至少 9 个 entry；清理后为 0/1/2～7 时立即降级，剩 8 个时重复 key 降级为 SmallState，
新 key 则补回第 9 个。因此 `drainExpiredEntries` 的第二个 expiry entry 必然存在。

最终稳态 JMH 为无 JFR、单线程、3 forks 配对测试（baseline -> final）：

| 活跃 key | hash 分布 | ns/op | 变化 |
|---:|---|---:|---:|
| 9 | spread | 19.282 -> 16.450 | +14.7% |
| 9 | collision | 33.434 -> 32.913 | +1.6% |
| 625 | spread | 19.430 -> 17.702 | +8.9% |
| 625 | collision | 1,291.770 -> 1,253.480 | +3.0% |
| 2,500 | spread | 19.550 -> 19.334 | +1.1% |
| 2,500 | collision | 5,258.862 -> 5,105.969 | +2.9% |
| 25,000 | spread | 18.113 -> 18.775 | -3.7% |
| 25,000 | collision | 58,320.468 -> 56,159.747 | +3.7% |

25,000-key spread 另以相同无 profiler 配置执行 5 forks 配对复测：
`19.936 +/- 1.396 -> 18.469 +/- 0.596 ns/op`，提升约 7.4%，且 final 各 fork 已收敛。
数据链路反例没有回退：非 Fuseable 的 HOT_KEYS 为 `17.522 -> 17.694 ops/s`（+1.0%），
UNIQUE 为 `9.369 -> 9.378 ops/s`（+0.1%）。

生命周期 SampleTime JMH（baseline -> final）：

| 场景 | key | ns/op | 倍数/改善 |
|---|---:|---:|---:|
| 全窗口过期后首条恢复 | 625 | 3,050.799 -> 53.688 | 56.8x |
| 全窗口过期后首条恢复 | 2,500 | 12,237.195 -> 84.785 | 144.3x |
| 全窗口过期后首条恢复 | 25,000 | 135,346.305 -> 384.958 | 351.6x |
| 25,000 降至低基数 | 8 | 145,690.860 -> 122,970.655 | +15.6% |
| 25,000 降至低基数 | 625 | 126,336.716 -> 122,267.688 | +3.2% |

验证结果：

- 定向：29 tests，0 failure，0 error；
- 最新 `origin/1.3`（`2585803f`）叠加 PR #97 与本轮最终实现：`mvn test` 共
  645 tests，0 failure，0 error，1 skipped；
- `git diff --check`：通过；
- JMH lifecycle 的 `Level.Invocation` setup 分配会被 GCProfiler 计入，因此不把该场景
  的 B/op 解释为被测恢复/缩容操作自身分配。

原始结果保留在 `target/distinct-duration-benchmark/`，不提交：

- `paired-steady-baseline-3f-t1.json` / `paired-steady-final-v6-3f-t1.json`；
- `no-duplicate-25000-baseline-5f-t1.json` / `no-duplicate-25000-optimized-v5-5f-t1.json`；
- `no-duplicate-datapath-baseline-3f-t1.json` / `no-duplicate-datapath-optimized-v6-3f-t1.json`；
- `lifecycle-baseline-t1.json` / `lifecycle-final-v6-3f-t1.json`。

## 500 万活跃 key 极限评测计划

### 目标与边界

评测 fixed-window 为 10 秒、逻辑输入速率为 500,000 条/秒的持续全唯一 key 场景。该口径
在稳态形成 5,000,000 个活跃 key，用于量化大 table 的 CPU cache、对象分配、GC 和恢复
尾延迟影响，不通过真实 sleep 或限速器把线程调度成本混入 store 算法。

本阶段先增加测试和生成 PR #97 当前实现与本轮生命周期实现的配对基准，不预设继续修改
生产代码。测试代码不进入 Surefire 默认测试集，不增加 `mvn test` 的固定内存和时间成本。

### 实施步骤

1. 在 `DistinctDurationFluxLargeStateBenchmark` 增加参数化极限状态：
   `windowSeconds=10`、`eventsPerSecond=500000`，按 2,000 ns 的逻辑事件间隔预填
   5,000,000 个 spread-hash key；
2. 稳态基准每次恰好推进一个事件间隔、淘汰一个最老 key 并插入一个预创建的新 key，
   分别采集 AverageTime、SampleTime p95/p99、B/op、GC count/time；
3. 增加 SingleShot 全窗口过期恢复基准：预填 5,000,000 key 后让最新 key 也超过 10 秒，
   测量下一条消息恢复成本；setup 分配不解释为被测操作 B/op；
4. benchmark runner 增加可选 heap 参数，默认仍为 2 GiB，极限场景显式使用 4 GiB，避免
   改变既有基准复现口径；
5. 将完全相同的 benchmark 源码应用到 PR #97 baseline 与当前实现，先 smoke，再执行
   单线程 3-fork 配对测试；原始 JSON 继续只保留在 `target/`；
6. 把实测 ns/event 换算为单核理论 events/s，以及 500,000 events/s 下的单核占用比例；
   把 B/event 换算为 MB/s，结合 GCProfiler 判断是否值得进一步复用到期 entry。

### 验收与决策

1. setup 后 store size 必须稳定为 5,000,000，稳态每次调用都必须成功插入；
2. 当前实现相对 PR #97 baseline 不得出现超过 5% 的稳定吞吐或 p99 回退；
3. 算法单核能力必须高于 500,000 events/s，并报告余量，不能只报告相对百分比；
4. 若 `gc.alloc.rate.norm` 仍约为一个 `LargeEntry`/event，报告 500,000 events/s 下的分配率；
   只有该分配已成为 CPU/GC 主导成本时，才进入 entry 复用设计，避免先改实现再找收益；
5. 复审继续覆盖 fixed-window、不续期、null、背压、fusion、discard、cleanup 和 LargeState
   数量不变量；极限测试不得引入 scheduler、并发调用同一 store 或设备专用逻辑。

### 实施与结果

测试按计划落在 `DistinctDurationFluxLargeStateBenchmark`：

1. `ExtremeRateState` 使用 10 秒窗口和 500,000 events/s 参数，按 2,000 ns 逻辑间隔
   预填 5,000,000 key；使用 5,000,001 个预创建 spread-hash key 循环，稳态每次恰好
   淘汰一个最老 key 并插入一个新 key；
2. `ExtremeIdleResumeState` 在每次 SingleShot 前重建 5,000,000-key 窗口，再把逻辑时钟
   推进 10 秒，确保最新 entry 也已过期；
3. 两个 setup 均校验 store size 为 5,000,000；benchmark runner 增加第六个 `heapGb`
   参数，默认 2 GiB 不变，本场景显式使用 4 GiB；
4. PR #97 baseline 和当前实现使用相同极限测试源码、JDK 21.0.10、G1、单线程、3 forks、
   2 x 2s warmup、5 x 2s measurement；4 GiB heap 已确认启用 compressed oops。

稳态结果（baseline -> current）：

| 指标 | baseline | current | 变化 |
|---|---:|---:|---:|
| AverageTime | 37.274 ns/event | 37.139 ns/event | +0.36% |
| 单核理论容量 | 26.83M events/s | 26.93M events/s | +0.36% |
| 500,000 events/s 单核占用 | 1.864% | 1.857% | -0.007pp |
| p50 | 68 ns | 67 ns | +1.5% |
| p95 | 86 ns | 90 ns | -4.7% |
| p99 | 184 ns | 187 ns | -1.6% |
| p99.9 | 10.821 us | 13.038 us | -20.5% |
| p99.99 | 26.517 us | 25.645 us | +3.3% |
| allocation | 32.001 B/event | 32.001 B/event | 持平 |

当前实现单核容量约为目标速率的 53.9 倍；平均 CPU 成本只占单核约 1.86%。固定分配
32 B/event 来自每次插入一个 `LargeEntry`，目标速率下约为 16.0 MB/s（15.3 MiB/s）。
以基准满速运行时，baseline/current 在 30 秒 measurement 中分别发生 18 次 GC，GC time
为 596/616 ms；分配与 GC 没有出现结构性变化。

current 的 SampleTime 记录到一个 2.253 ms 最大值，baseline 最大值为 69.504 us，导致
sample mean 为 `95.946 -> 100.923 ns`（-5.2%）。该样本低于百万分之二，且同一恒定稳态
路径没有触发 resize、shrink 或批量过期；p99 只回退 1.6%，p99.99 反而改善 3.3%，现有
证据不足以把该单点离群归因到本轮实现。保留为 JVM/OS/GC 尾延迟残余风险，不据此增加
生产分支。

全窗口过期后的首条恢复 SingleShot（baseline -> current）：

| 指标 | baseline | current | 提升 |
|---|---:|---:|---:|
| mean | 31.128 ms | 6.483 us | 4,802x |
| p50 | 31.308 ms | 6.391 us | 4,899x |
| p95/p99 | 32.049 ms | 10.258 us | 3,124x |

SingleShot 的 `227,131,037 B/op` 来自每次调用前预填 5,000,000 entry 的
`Level.Invocation` setup，baseline/current 完全相同，不能解释为恢复操作分配。该数值
可用于确认构造整个 store 累计分配约 227.1 MB；在 compressed oops 下，稳态保留结构
估算为 5,000,000 x 32 B entry 加 8,388,608 槽 table，约 193.6 MB，不含业务 key。

结论：本轮生命周期优化在 500 万活跃 key 稳态下没有吞吐、p99 或分配回退，并把全窗口
恢复从约 31 ms 降到微秒级。`LargeEntry` 复用理论上可消除 16 MB/s 目标分配，但当前 CPU
余量约 53.9 倍、满速 GC 时间约占 measurement 的 2%，分配尚未成为主导瓶颈；暂不增加
可变 entry 和复用状态，避免扩大 fixed-window 正确性与对象生命周期风险。

原始结果保留在 `target/distinct-duration-benchmark/`，不提交：

- baseline：`extreme-rate-baseline-3f-t1.json`、`extreme-idle-baseline-3f-t1.json`；
- current：`extreme-rate-current-3f-t1.json`、`extreme-idle-current-3f-t1.json`。

- 本阶段实现 commit：`d19d6370e9c41934fb86dc56f3cd3a5cd67f8004`。
- Pull Request：https://github.com/jetlinks/jetlinks-core/pull/97
