# TopicFinder 精确 Topic 查找优化

## 目标与范围

本优化面向 `TopicFinder` 的高频精确发布场景：在保持精确路径、`*`、`**`、String 与
`SeparatedCharSequence` 既有匹配语义的前提下，减少通用 DFS 的无效分支和 wildcard 子节点
Map 查找。

影响范围：

- owning module：`jetlinks-core`
- 生产代码：
  - `src/main/java/org/jetlinks/core/topic/Topic.java`
  - `src/main/java/org/jetlinks/core/topic/TopicFinder.java`
- 测试与基准：
  - `src/test/java/org/jetlinks/core/topic/TopicFinderTest.java`
  - `src/test/java/org/jetlinks/core/benchmark/TopicFinderJmhBenchmark.java`
  - `src/test/java/org/jetlinks/core/benchmark/TopicFinderJmhBenchmarkTest.java`

不修改 Topic、订阅计数、cleanup、序列化或公开路由 API 语义；不引入后台任务、线程池、
响应式操作符或逐次查找缓存。

## 实现结论

最终实现包含两层优化：

1. 搜索 Topic 不含 wildcard 时进入精确查找路径，仍同时遍历订阅树中的 exact、`*`、`**`
   分支；搜索 Topic 自身含 `*` 或 `**` 时继续使用原 DFS 语义。
2. `Topic` 缓存直属 `*`、`**` 子节点，避免大多数节点每层额外执行两次
   `ConcurrentHashMap.get`。缓存只在节点已进入 child Map 后发布，并在 `cleanup()`、`clean()`
   时同步失效和支持重新 append。

新增 wildcard getter 已收窄为包可见，没有扩大公共 API。`getChildrenMap()` 既有签名和返回行为
未修改；当前 core 与本工作区未发现外部写入调用。直接绕过 `append/cleanup/clean` 修改其返回 Map
不是受支持的树更新方式，可能绕过 Topic 自身的生命周期维护。

为避免错误基准结论，JMH 改为确定性样本、独立 fork、平均时延模式，并提供显式系统属性控制
fork、warmup、measurement、单轮时长、include 和 JSON 输出路径；普通 `mvn test` 默认跳过耗时
基准。

## 正式 JMH 对比

基线为最新 `1.3` 的 `c2ac1275`，候选为 PR 精确路径加 wildcard 缓存。环境为 macOS x86_64、
Temurin JDK 21.0.10、G1、`-Xms2g -Xmx2g`、单线程；3 forks、3 次 1 秒 warmup、5 次 1 秒
measurement，并启用 `GCProfiler`。提升按 `(baseline - candidate) / baseline` 计算。

| 场景 | 树类型 | 1.3 基线 ns/op | 优化后 ns/op | 时延下降/吞吐提升 |
|---|---|---:|---:|---:|
| exact Separated | EXACT_ONLY | 273.636 | 226.565 | 17.20% |
| exact Separated | MIXED | 417.599 | 342.765 | 17.92% |
| exact String | EXACT_ONLY | 691.273 | 588.837 | 14.82% |
| exact String | MIXED | 889.586 | 775.167 | 12.86% |
| miss Separated | EXACT_ONLY | 166.066 | 116.159 | 30.05% |
| miss Separated | MIXED | 289.487 | 204.213 | 29.46% |
| miss String | EXACT_ONLY | 627.764 | 551.747 | 12.11% |
| miss String | MIXED | 765.178 | 691.480 | 9.63% |
| wildcard Separated | EXACT_ONLY | 23999.502 | 19557.881 | 18.51% |
| wildcard Separated | MIXED | 28987.626 | 28652.491 | 1.16% |
| wildcard String | EXACT_ONLY | 18450.014 | 17327.696 | 6.08% |
| wildcard String | MIXED | 24788.087 | 24481.088 | 1.24% |

主要精确发布和 miss 场景获得 9.63%～30.05% 的稳定时延下降；搜索侧 wildcard 的混合树
保持正向，没有超过 5% 的稳定回退。

分配结果：

- exact Separated 与 miss Separated 均约为 0 B/op，优化前后不变；
- exact String 约 472 B/op、miss String 约 480 B/op，优化前后不变；
- wildcard Separated MIXED 约 56 B/op、wildcard String MIXED 约 528 B/op，优化前后不变；
- wildcard String EXACT_ONLY 在 JDK 21 C2 下存在 472/528 B/op 双态。追加 5 forks、5 次 warmup、
  7 次 measurement 后，基线 5 个 fork 中 2 个为 472 B/op、3 个为 528 B/op，候选为
  528 B/op；候选时延仍由 18789.180 降至 17553.219 ns/op（6.58%）。该 56 B 差异来自
  fork 间标量替换双态，不是实现新增的显式对象；MIXED 场景稳定保持约 528 B/op。

原始 JSON 位于本地 `target/`，属于构建产物，不提交。

## 常驻内存

JOL 在相同 JVM 布局下测得：

- 最新 `1.3` 的 `Topic` 实例为 40 B；
- 缓存两个 wildcard 子节点后为 48 B；
- 每个 Topic 节点增加 8 B，每百万节点约增加 7.63 MiB。

使用与 JMH 相同结构生成完整 Topic 树并执行 `GraphLayout`：

| 树类型 | Topic 数 | 1.3 retained heap | 优化后 retained heap | 增量 |
|---|---:|---:|---:|---:|
| EXACT_ONLY | 131586 | 22379056 B | 23431744 B | 1052688 B（4.70%） |
| MIXED | 165634 | 30456464 B | 31781536 B | 1325072 B（4.35%） |

该成本由全部 Topic 节点承担，但换取精确 Separated 主路径 17%～18%、miss 主路径约 30% 的稳定
收益。没有额外 holder、Map、数组、后台缓存或清理任务。

## 语义与生命周期验证

`TopicFinderTest` 新增和扩展以下验证：

- 精确发布同时命中 exact、`*`、`**` 订阅；
- 多组真实 tenant/device/message 形状按 `TopicUtils.match` 参考模型校验；
- String 与 `SharedPathString` 结果一致；
- 嵌套 wildcard、搜索侧 wildcard 与无重复节点投递；
- wildcard 订阅 cleanup 后不再命中，重新 append 后恢复；
- `clean()` 后重新建树可正常命中。

验证结果：

- 定向测试：`mvn -o -Dtest=TopicFinderTest,TopicTest test`，54 项通过；
- Topic 路由与 Trace 回归：
  `mvn -o -Dtest=TopicRouteTest,TraceHolderTest,DeviceTracerTest test`，12 项通过；
- 全量测试：`mvn -o test`，653 项，0 failure、0 error、2 skipped；其中耗时 JMH 入口按设计
  默认跳过；
- JaCoCo（全量测试后）：`TopicFinder` 行覆盖 177/266（66.5%）、分支覆盖
  110/188（58.5%）；`Topic` 行覆盖 188/308（61.0%）、分支覆盖 129/218（59.2%）；
- `git diff --check` 通过；
- 集成测试不适用：本次只修改进程内同步 Topic 树查找，不涉及数据库、中间件、集群协议或启动装配。

本优化是同步、进程内查找，不涉及数据库、外部 I/O 或跨线程响应式上下文，因此不新增
TraceHolder；未新增长期运行管理器、队列或后台资源，因此不新增 MBean。

## 交付信息

- 最新 `1.3` 基线：`c2ac12755760e738c8ec135e6388946a447ca20d`
- 生命周期与验证提交：`56f53527`
- Pull Request：https://github.com/jetlinks/jetlinks-core/pull/85
