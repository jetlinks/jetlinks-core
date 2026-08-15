# SharedPathString 性能优化计划

## 目标

在保持 `SharedPathString`、`SeparatedCharSequence` 现有可观察语义的前提下，先建立可重复的
JMH 基准，再优化设备消息 Topic 构造中的高频路径，并使用相同环境对比优化前后吞吐、时延和
`gc.alloc.rate.norm`。

## 影响范围

- owning module：`jetlinks-core`
- 重点代码：
  - `src/main/java/org/jetlinks/core/lang/AbstractSeparatedCharSequence.java`
  - `src/main/java/org/jetlinks/core/lang/AppendSeparatedCharSequence*.java`
  - `src/main/java/org/jetlinks/core/lang/SharedPathString.java`
  - `src/test/java/org/jetlinks/core/lang/SeparatedStringTest.java`
  - 新增的 `src/test/java/org/jetlinks/core/benchmark/SharedPathStringBenchmark.java`
- 设备场景验证参考：`DeviceMessageConnector` 使用的 property、online/offline、event、child
  Topic 形状。

## 保持不变的语义

1. 不修改 `equals`、`hashCode`、`compareTo`、`contentEquals` 的既有定义。
2. 不修改 `SharedPathString.of(String)`、`intern()`、split cache 和 segment intern 的既有语义。
3. `append` 输入包含 `/`、以 `/` 开头、空字符串、字符分隔符时，生成的 segment、`size()`、
   `get()`、`length()` 和 `toString()` 与当前实现一致。
4. 不改变现有异常、序列化和具体 Topic 匹配行为；不引入新的公共 SPI。
5. 不修改 EventBus 批量 API、Components 发布实现或 Device Manager 生产代码。

## 不做什么

- 本轮不修复 `empty().length()`、跨实现内容相等、数组可变性等既有契约问题。
- 不移除或改造成有界 intern cache，不改变高基数 Topic 的缓存策略。
- 不引入 ThreadLocal、对象池、scheduler 或响应式缓存。
- 不以非 fork smoke 数据作为最终性能结论。

## 实施步骤

1. 增加独立 JMH 基准，覆盖：
   - 静态后缀 property/status Topic；
   - 动态单段 eventId/childDeviceId append；
   - 含分隔符和前导分隔符的 append；
   - append 深度 `1/4/8`；
   - 构造、构造后 `hashCode()`、`contentEquals()`；
   - intern 命中与不使用 intern 的解析对照。
2. 在当前 HEAD 上使用固定 JDK、JVM 参数、fork/warmup/measurement 和 GC profiler 执行基准，
   保存原始 JSON 作为基线。
3. 根据基线只优化确认的热点。首选方案是在单 segment append 中避免不必要的完整 path split，
   同时保留当前 segment intern 和包含分隔符时的既有解析行为；必要时缓存 composite 的稳定尺寸，
   但不扩大改动范围。
4. 补充行为等价测试，覆盖基准中的所有 Topic 形状，并校验 concrete class 相关的
   `equals/hashCode/compareTo` 结果未变化。
5. 在相同环境重新执行完整 JMH 矩阵，报告每个场景的 ns/op、B/op、变化百分比和误差区间；
   再运行 core 定向测试与编译。

## 风险与门禁

- 最大风险是“单段 fast path”意外改变 segment intern、前导/尾部分隔符或多段展开语义；必须用
  优化前结果快照和行为测试双重门禁。
- JMH 必须使用独立 fork；non-fork 结果只用于发现热点。
- 设备 property/status 当前已走结构化静态后缀路径，不允许为优化 event 动态段导致其吞吐或
  B/op 明显回退。
- 若优化仅改善微基准但增加完整 Topic 常驻内存，或正式多 fork 结果无稳定收益，则撤回生产改动，
  只保留基准。

## 验证方式

- JMH：至少 2 fork、3 次 warmup、5 次 measurement、JDK 21、G1、固定堆，启用 `GCProfiler`；
  关键场景补 JFR。
- 单元测试：`SharedPathStringTest`、`SeparatedStringTest` 及新增语义回归。
- 构建：`mvn -q -DskipTests compile` 和相关定向测试。
- 结果完成后回填本文件，包括基线、优化后结果、最终代码落点和剩余风险。

## 实施结果

最终只修改 `AbstractSeparatedCharSequence.append(CharSequence)` 的单 segment 分支：输入先按原语义
执行 `toString()` 快照；非空且不包含当前 separator 时直接执行 segment intern，不再调用完整 path
split；空字符串、包含 separator 的字符串以及已经结构化的 `SeparatedCharSequence` 仍走原路径。

该实现保留了以下可观察行为：

- 单 segment 仍由 `RecyclerUtils.intern` 共享引用；
- 返回类型仍为 `AppendSeparatedCharSequence`；
- 多 segment、前导/尾随 separator 和空字符串的展开、`size()`、`get()`、`toString()` 不变；
- 可变 `CharSequence` 仍在 append 时形成字符串快照；
- `equals`、`hashCode`、`compareTo` 和 `contentEquals` 未修改。

最终代码落点：

- 生产优化：`src/main/java/org/jetlinks/core/lang/AbstractSeparatedCharSequence.java`
- 行为回归：`src/test/java/org/jetlinks/core/lang/SeparatedStringTest.java`
- 可重复基准：`src/test/java/org/jetlinks/core/benchmark/SharedPathStringBenchmark.java`

本次不新增缓存、常驻任务或公共契约，因此不需要 MBean、TraceHolder 或 i18n 变更。

## 正式 JMH 对比

环境：macOS 15.7.4 x86_64、Temurin JDK 21.0.10、G1、`-Xms1g -Xmx1g`；每组
2 fork、3 次 1 秒 warmup、5 次 1 秒 measurement，并启用 `GCProfiler`。表中误差为 JMH
99.9% 置信区间；吞吐倍数由相同操作的 `baseline ns/op / optimized ns/op` 换算。

| 场景 | 基线 ns/op | 优化后 ns/op | 时延下降 | 吞吐倍数 | B/op 基线→优化后 | 分配下降 |
|---|---:|---:|---:|---:|---:|---:|
| 单 segment append | 247.069 ± 4.210 | 28.841 ± 0.216 | 88.3% | 8.57x（+756.7%） | 160.007 → 24.001 | 85.0% |
| 单 segment append + hash | 267.861 ± 8.554 | 46.276 ± 1.339 | 82.7% | 5.79x（+478.8%） | 160.007 → ≈0 | ≈100% |
| device event Topic | 251.150 ± 9.715 | 42.246 ± 0.389 | 83.2% | 5.94x（+494.5%） | 232.010 → 96.004 | 58.6% |
| device event Topic + hash | 272.102 ± 12.775 | 65.587 ± 0.649 | 75.9% | 4.15x（+314.9%） | 232.011 → 96.004 | 58.6% |
| append depth 1 + hash | 240.333 ± 10.068 | 38.515 ± 1.086 | 84.0% | 6.24x（+524.0%） | 160.007 → 24.001 | 85.0% |
| append depth 4 + hash | 1075.960 ± 74.013 | 225.092 ± 9.071 | 79.1% | 4.78x（+378.0%） | 640.026 → 96.004 | 85.0% |
| append depth 8 + hash | 2449.212 ± 55.405 | 773.194 ± 56.716 | 68.4% | 3.17x（+216.8%） | 1280.059 → 192.009 | 85.0% |
| 完整 path append | 710.074 ± 8.877 | 727.111 ± 9.638 | -2.4% | 0.98x（-2.3%） | 640.029 → 640.029 | 0% |
| device property Topic | 9.069 ± 0.070 | 8.785 ± 0.124 | 3.1% | 1.03x（+3.2%） | 72.003 → 72.003 | 0% |
| device property Topic + hash | 33.622 ± 1.490 | 32.225 ± 0.229 | 4.2% | 1.04x（+4.3%） | 72.003 → 72.003 | 0% |
| shared parse | 55.293 ± 1.291 | 54.107 ± 1.048 | 2.1% | 1.02x（+2.2%） | 24.001 → 24.001 | 0% |
| unshared parse | 472.451 ± 3.478 | 459.363 ± 11.371 | 2.8% | 1.03x（+2.8%） | 480.022 → 480.023 | 0% |
| structured contentEquals | 883.837 ± 31.669 | 850.617 ± 6.879 | 3.8% | 1.04x（+3.9%） | ≈0 → ≈0 | 0% |

结论：收益集中在预期的动态单 segment 热路径。property 静态后缀、parse 和
`contentEquals` 没有结构性分配变化；完整 path append 不命中 fast path，B/op 完全不变，约
2.3% 的吞吐差异按跨进程运行噪声处理，不扩大优化范围。

## JFR 复核

对优化后的 `appendSingleSegment`、`eventTopic` 各执行 1 fork、3 warmup、5 measurement 的
JFR profile：

- `appendSingleSegment` 的采样分配 99.82% 为最终结果对象
  `AppendSeparatedCharSequence`；
- `eventTopic` 的采样分配 99.89% 为既有 product/device 替换对象
  `ReplacedSeparatedCharSequence2`；
- 两个场景均未再出现 `TopicUtils.split` 所产生的临时数组、分段字符串和结构化 path 对象主导分配。

JFR 与 `GCProfiler` 的 160 → 24 B/op、232 → 96 B/op 结论一致，说明收益来自删除单 segment
场景的无效完整 path split，而不是调度、缓存或测量绕过。

## 验证结果

- 定向测试：`mvn -o -q -Dtest=SeparatedStringTest,SharedPathStringTest test`，12/12 通过；
- 全量测试：`mvn -o -q test`，632 项，0 failure、0 error、1 skipped；
- 编译：`mvn -o -q -DskipTests compile` 通过；
- 格式：`git diff --check` 通过；
- JaCoCo（全量测试后）：`AbstractSeparatedCharSequence` 行覆盖 89/121（73.6%），分支覆盖
  61/90（67.8%）。

原始基准 JSON 和 JFR 位于 `target/shared-path-string-benchmark/`，属于本地构建产物，不提交；
正式数值已回填本文件。现有回归用例未发现语义变化；基准只代表 Topic 构造本身，端到端 EventBus 投递收益
仍会受订阅匹配、序列化和消费端成本影响。

## 第二阶段：Append 复合遍历优化计划

目标限定在 `AppendSeparatedCharSequence` 和 `AppendSeparatedCharSequenceX` 形成的复合链：

1. 补充 append 深度 `1/4/8` 的 `length()`、`toString()`、原始字符串 `contentEquals()` 基准，
   与现有 `hashCode()` 深度基准共同建立优化前数据；
2. 使用包内、无额外对象字段的 segment 线性遍历方法，避免 `hashCode()`、`toString()`、
   `length()` 经由 `get(i)` 反复递归 source 链；
3. 保持具体返回类型、对象大小、segment intern、hash 结果、前导空 segment 合并以及
   `equals/compareTo/contentEquals` 结果不变；
4. 不增加 `$size`、字符串/数组快照、Topic 缓存或节点压平，避免百万 Topic 场景增加常驻内存；
5. 使用同一 JMH 参数对比深度场景，并以单段 append 的 24 B/op 不回退作为内存门禁。

风险主要在 `AppendSeparatedCharSequenceX.ignoreFirst` 的前导空 segment 语义，以及 hash 必须以
最外层具体 class 为 seed。实施前后将使用不同 separator、空段、多段 append、深度链和 hash
快照测试共同校验。

### 第二阶段实施结果

最终实现没有增加任何对象字段：

- `appendHash` 由 append 节点按 source → 当前 segment 顺序计算，保留最外层具体 class hash seed；
- `contentLength` 和 `appendTo` 直接遍历 source 链，不再为每个索引重复调用递归 `get(i)`；
- 原始字符串 `contentEquals` 改为按 segment 和字符单次比较，不再对每个字符重新执行
  `this.charAt(i)` 的完整 segment 搜索；
- 单层 append hash 保留原索引路径，使 JIT 仍能标量替换短生命周期节点；仅嵌套单段 append 链
  使用线性 hash。全量线性 hash 的中间方案使 `appendSingleSegmentHash` 从 ≈0 增加到 24 B/op，
  因此未保留。

没有实施节点压平、`$size` 字段、字符串/数组快照或缓存；具体 class、对象大小和常驻内存不变。

### 第二阶段正式 JMH 对比

环境和参数与第一阶段相同：Temurin JDK 21.0.10、G1、固定 1 GiB 堆、2 fork、3 次 1 秒
warmup、5 次 1 秒 measurement、`GCProfiler`。吞吐提升由基线与最终 ns/op 的倒数换算。

| 操作 | 深度 | 基线 ns/op | 最终 ns/op | 时延下降 | 吞吐提升 | B/op 基线→最终 |
|---|---:|---:|---:|---:|---:|---:|
| contentEquals | 1 | 187.411 ± 6.769 | 57.426 ± 0.390 | 69.4% | 3.26x（+226.4%） | 24.001 → 24.001 |
| contentEquals | 4 | 5602.941 ± 35.965 | 294.092 ± 5.894 | 94.8% | 19.05x（+1805.2%） | 96.032 → 96.005 |
| contentEquals | 8 | 51187.030 ± 335.342 | 874.755 ± 59.942 | 98.3% | 58.52x（+5751.6%） | 194.299 → 192.010 |
| hashCode | 1 | 38.530 ± 0.390 | 37.986 ± 0.184 | 1.4% | 1.01x（+1.4%） | 24.001 → 24.001 |
| hashCode | 4 | 230.002 ± 3.480 | 135.571 ± 3.400 | 41.1% | 1.70x（+69.7%） | 96.004 → 96.004 |
| hashCode | 8 | 791.434 ± 59.226 | 319.917 ± 1.951 | 59.6% | 2.47x（+147.4%） | 192.009 → 192.009 |
| length | 1 | 32.475 ± 0.135 | 29.582 ± 0.403 | 8.9% | 1.10x（+9.8%） | 24.001 → 24.001 |
| length | 4 | 226.474 ± 5.928 | 121.595 ± 0.802 | 46.3% | 1.86x（+86.3%） | 96.004 → 96.004 |
| length | 8 | 799.527 ± 74.074 | 277.180 ± 2.310 | 65.3% | 2.88x（+188.5%） | 192.009 → 192.008 |
| toString | 1 | 104.648 ± 6.082 | 105.488 ± 2.449 | -0.8% | 0.99x（-0.8%） | 96.004 → 96.004 |
| toString | 4 | 325.584 ± 27.023 | 249.221 ± 2.710 | 23.5% | 1.31x（+30.6%） | 216.010 → 216.009 |
| toString | 8 | 859.465 ± 46.995 | 487.849 ± 5.155 | 43.2% | 1.76x（+76.2%） | 384.017 → 384.017 |

浅链与既有设备场景门禁：

| 场景 | 基线 ns/op | 最终 ns/op | B/op 基线→最终 |
|---|---:|---:|---:|
| appendSingleSegment | 29.536 ± 0.642 | 29.264 ± 0.196 | 24.001 → 24.001 |
| appendSingleSegmentHash | 45.408 ± 0.681 | 44.771 ± 0.457 | ≈0 → ≈0 |
| eventTopic | 43.433 ± 0.351 | 43.740 ± 0.263 | 96.004 → 96.004 |
| eventTopicHash | 65.716 ± 5.832 | 65.962 ± 0.625 | 96.004 → 96.004 |
| appendFullPath | 747.221 ± 4.601 | 750.433 ± 6.103 | 640.026 → 640.026 |

浅链差异均在约 ±1% 和置信区间内，没有结构性回退。深度 8 JFR 中，`contentEquals` 与
`hashCode` 的采样分配分别有 98.28% 和 99.29% 来自必须创建的
`AppendSeparatedCharSequence` 节点，未出现新的缓存、数组、遍历状态或字符串快照分配。

### 第二阶段验证结果

- 行为测试覆盖单/多 segment、自定义 separator、空段、`ignoreFirst`、可变输入快照、深度 8
  链，以及按旧公式动态计算的 hash 结果；
- 定向测试：`SeparatedStringTest`、`SharedPathStringTest` 共 12 项通过；
- 全量测试：632 项，0 failure、0 error、1 skipped；
- `mvn -o -q -DskipTests compile`、`git diff --check` 通过；
- JaCoCo：`AbstractSeparatedCharSequence` 行 114/147、分支 77/108；
  `AppendSeparatedCharSequence` 行 17/20；`AppendSeparatedCharSequenceX` 行 30/34。

第二阶段原始数据位于本地构建目录：

- `target/shared-path-string-benchmark/append-traversal-baseline.json`
- `target/shared-path-string-benchmark/append-traversal-final.json`
- `target/shared-path-string-benchmark/jfr-append-final/`

这些构建产物不提交，正式结果已回填本文件。

## 交付信息

- 实现提交：`eb9b582a4b535ad2e4605c6a199cd02e66fc8ac5`
- Pull Request：https://github.com/jetlinks/jetlinks-core/pull/96
