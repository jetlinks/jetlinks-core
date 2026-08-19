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
  - `src/test/java/org/jetlinks/core/topic/TopicFinderOverloadTest.java`
  - `src/test/java/org/jetlinks/core/topic/TopicContractTest.java`
  - `src/test/java/org/jetlinks/core/topic/TopicLifecycleConcurrencyTest.java`
  - `src/test/java/org/jetlinks/core/benchmark/TopicFinderJmhBenchmark.java`
  - `src/test/java/org/jetlinks/core/benchmark/TopicFinderJmhBenchmarkTest.java`

不修改 Topic、订阅计数、cleanup、序列化或公开路由 API 语义；不引入后台任务、线程池、
响应式操作符或逐次查找缓存。

追加优化继续保持上述兼容边界，并收敛以下实现约束：

- append、subscribe、cleanup、clean 并发时，子节点、订阅容器和 wildcard 索引必须在操作返回后
  保持一致，不允许出现游离容器、永久漏投或已删除节点继续命中；
- 平台实际使用的 `findTopic(topic, sink, end)` 重载需同时评估逐次分配与吞吐，不能为 0 B/op
  接受明显时延回退；
- String、普通 `CharSequence`、`SeparatedCharSequence` 和数组重载统一支持有无前导 `/`；
- lookup-only 不得创建空子节点 Map，搜索侧 `**` 的去重容器不得因一次大查询长期保留大数组；
- wildcard 索引在保持查找收益的前提下压缩节点字段，最终方案由相同 JMH/JOL 场景决定。

## 实现结论

最终实现包含以下优化：

1. 搜索 Topic 不含 wildcard 时进入精确查找路径，仍同时遍历订阅树中的 exact、`*`、`**`
   分支；搜索 Topic 自身含 `*` 或 `**` 时继续使用原 DFS 语义。
2. `Topic` 继续使用直属 `*`、`**` 字段，避免大多数节点每层额外执行两次
   `ConcurrentHashMap.get`。JMH 已否决单字段 holder：混合树精确 Separated 查找回退超过 5%。
3. 删除 Topic 字符串与 hash 缓存；搜索侧 `**` 改用可缩容 identity set，大查询回收时替换内部
   大表。最终 Topic 恢复到 40 B，同时避免一次大 wildcard 查询长期保留大数组。
4. append、subscribe、cleanup、clean 在节点锁内维护容器与 wildcard 索引；被清理节点复用
   subscribers 字段保存 detached 标记，旧 Topic 引用的后续 append/subscribe 会重建到当前树。
5. lookup-only 直接遍历现有 Map，不再为 miss 创建空 Map；String、普通 CharSequence、
   SeparatedCharSequence 与数组统一支持有无前导 `/`。

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
| exact Separated | EXACT_ONLY | 273.636 | 223.678 | 18.26% |
| exact Separated | MIXED | 417.599 | 365.960 | 12.37% |
| exact String | EXACT_ONLY | 691.273 | 588.837 | 14.82% |
| exact String | MIXED | 889.586 | 775.167 | 12.86% |
| miss Separated | EXACT_ONLY | 166.066 | 116.765 | 29.69% |
| miss Separated | MIXED | 289.487 | 224.901 | 22.31% |
| miss String | EXACT_ONLY | 627.764 | 551.747 | 12.11% |
| miss String | MIXED | 765.178 | 691.480 | 9.63% |
| wildcard Separated | EXACT_ONLY | 23999.502 | 22528.173 | 6.13% |
| wildcard Separated | MIXED | 28987.626 | 27736.013 | 4.32% |
| wildcard String | EXACT_ONLY | 18450.014 | 17327.696 | 6.08% |
| wildcard String | MIXED | 24788.087 | 24481.088 | 1.24% |

主要精确发布和 miss 场景获得 9.63%～29.69% 的稳定时延下降；搜索侧 wildcard 的混合树
保持正向，没有超过 5% 的稳定回退。

分配结果：

- 四参数 exact Separated 与 miss Separated 均约为 0 B/op，优化前后不变；
- 平台实际 `findTopic(topic, sink, end)` Separated 重载约 16 B/op。0 B 静态适配器正式复测导致
  EXACT_ONLY/MIXED 时延回退约 18%/14.5%，因此最终保留捕获适配器以吞吐优先；String 实际重载
  从约 488 B/op 降到 472 B/op，时延约回退 3%；
- exact String 约 472 B/op、miss String 约 480 B/op，优化前后不变；
- wildcard Separated MIXED 约 56 B/op、wildcard String MIXED 约 528 B/op，优化前后不变；
- wildcard String EXACT_ONLY 在 JDK 21 C2 下存在 472/528 B/op 双态。追加 5 forks、5 次 warmup、
  7 次 measurement 后，基线 5 个 fork 中 2 个为 472 B/op、3 个为 528 B/op，候选为
  528 B/op；候选时延仍由 18789.180 降至 17553.219 ns/op（6.58%）。该 56 B 差异来自
  fork 间标量替换双态，不是实现新增的显式对象；MIXED 场景稳定保持约 528 B/op。

原始 JSON 位于本地 `target/`，属于构建产物，不提交。

## 常驻内存

JOL 在相同 JVM 布局下测得最终 `Topic` 实例为 40 B，与最新 `1.3` 一致；相对本 PR 之前的
48 B 实现，每百万 Topic 节点减少约 7.63 MiB。

使用与 JMH 相同结构生成完整 Topic 树并执行 `GraphLayout`：

| 树类型 | Topic 数 | 1.3 retained heap | 优化后 retained heap | 增量 |
|---|---:|---:|---:|---:|
| EXACT_ONLY | 131586 | 22379056 B | 22379056 B | 0 B |
| MIXED | 165634 | 30456464 B | 30456464 B | 0 B |

最终没有每节点额外内存成本，也没有额外 holder、后台缓存或清理任务。`**` 去重 holder 在集合
超过 4096 项时直接替换内部 identity set，小查询继续复用已有小表。

## 语义与生命周期验证

`TopicFinderTest` 新增和扩展以下验证：

- 精确发布同时命中 exact、`*`、`**` 订阅；
- 多组真实 tenant/device/message 形状按 `TopicUtils.match` 参考模型校验；
- String 与 `SharedPathString` 结果一致；
- 嵌套 wildcard、搜索侧 wildcard 与无重复节点投递；
- wildcard 订阅 cleanup 后不再命中，重新 append 后恢复；
- `clean()` 后重新建树可正常命中。
- cleanup/clean 与 append/subscribe 并发压力、旧节点引用重建和最终 wildcard 索引一致性；
- 所有重载无前导 `/`、lookup miss 不创建 Map、大 `**` 去重表回收后缩容。

验证结果：

- 定向测试覆盖 TopicFinder、重载、Topic 契约、生命周期并发与路由，75 项通过；
- Topic 路由与 Trace 回归：
  `mvn -o -Dtest=TopicRouteTest,TraceHolderTest,DeviceTracerTest test`，12 项通过；
- 全量测试：`mvn -q test`，667 项，0 failure、0 error、2 skipped；其中耗时 JMH 入口按设计
  默认跳过；
- JaCoCo（全量测试后）：`TopicFinder` 行覆盖 255/269（94.8%）、分支覆盖
  165/196（84.2%）；`Topic` 行覆盖 373/404（92.3%）、分支覆盖 224/272（82.4%）；
- `git diff --check` 通过；
- 集成测试不适用：本次只修改进程内同步 Topic 树查找，不涉及数据库、中间件、集群协议或启动装配。

本优化是同步、进程内查找，不涉及数据库、外部 I/O 或跨线程响应式上下文，因此不新增
TraceHolder；未新增长期运行管理器、队列或后台资源，因此不新增 MBean。

## 交付信息

- 最新 `1.3` 基线：`c2ac12755760e738c8ec135e6388946a447ca20d`
- 生命周期与验证提交：`56f53527`
- Pull Request：https://github.com/jetlinks/jetlinks-core/pull/85
