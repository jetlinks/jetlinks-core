# 设备响应式读取性能优化

## 目标与范围

优化 `DefaultDeviceOperator`、`DefaultDeviceProductOperator` 高频读取中的 Reactor 操作符与临时对象，
保持配置缓存、产品回退、协议动态变更、错误、Context、请求和取消语义不变。

影响范围：

- owning module：`jetlinks-core`
- 配置读取：`StorageConfigurable`、`MonoConfigRead`、`MonoConfigsRead`
- 类型化设备配置：`DeviceOperator#getSelfConfig(ConfigKey)`、`getSelfConfigs(ConfigKey...)`
- 协议读取：`DefaultDeviceOperator#getProtocol`、`DefaultDeviceProductOperator#getProtocol`
- 契约测试：配置读取和新增协议 Mono 的同步、异步、空值、错误、Context、请求及取消场景

不修改配置缓存、产品缓存、协议实例缓存或失效逻辑；不缓存配置值、产品或协议结果；不引入线程池、
对象池、阻塞调用或跨订阅共享状态。

## 优化前基线

环境：JDK 17.0.18、Reactor 3.7.8、G1、4 线程、独立 JVM、3 轮中位数，L1 命中且
`remote_reads=0`。

| 场景 | QPS | B/op |
|---|---:|---:|
| `getSelfConfig(String)` | 228.05M | 48 |
| `getSelfConfig(ConfigKey)` | 134.73M | 144 |
| `getSelfConfigs(String ×1)` | 104.54M | 232 |
| `getSelfConfigs(String ×4)` | 62.45M | 360 |
| `getSelfConfigs(ConfigKey ×1)` | 41.75M | 672 |
| `getSelfConfigs(ConfigKey ×4)` | 28.46M | 896 |
| `getProtocol` 设备协议命中 | 96.34M | 160 |
| `getProtocol` 产品协议回退 | 26.77M | 480 |

JFR 显示类型化单值读取主要额外分配为转换 lambda 和 `MapFuseableSubscriber`；类型化批量读取
主要额外分配来自 stream、collector、`HashSet` 及其节点；协议读取主要额外分配来自
`MonoFlatMap` 与 `SwitchIfEmptySubscriber`。

## 实施步骤

1. 让自身配置读取也统一使用现有自定义配置 Mono，同步来源直接读取，异步来源继续交由 Reactor。
2. 类型化单值读取在配置 Mono 内完成 `Value` 转换，移除外层 `map`；保持原空值转换语义。
3. 类型化批量读取使用直接循环构造键集合，删除 stream/collector 临时对象。
4. 新增设备和产品共用的协议 Mono，按需读取当前协议配置；设备配置为空或协议查找为空时再回退产品。
5. 补充同步/异步、空值、错误、Context、请求、取消和重复订阅测试。
6. 使用相同 QPS/JFR 场景复测，并回填最终结果。

## 风险与验证

- 自定义 Mono 必须在首次合法请求后才读取，取消必须传播到当前活动源。
- 设备协议存在但协议源返回 empty 时，仍需按原逻辑回退产品；错误不得触发回退。
- 每次订阅重新读取当前配置，禁止把配置或协议结果保存到操作符实例。
- 验证包括定向单元测试、core 编译、`git diff --check`、4 线程 QPS 和代表场景 JFR。

## 实施结果

实现落点：

- `src/main/java/org/jetlinks/core/defaults/MonoProtocolSupport.java`：按请求解析设备或产品协议，
  同步来源直接读取，异步阶段继续由 Reactor 订阅管理。
- `src/main/java/org/jetlinks/core/config/MonoConfigRead.java`：类型化单值转换合并到配置 Mono；
  父级回退直接调用父级类型化读取，避免回退后二次创建转换链。
- `src/main/java/org/jetlinks/core/config/StorageConfigurable.java`：自身配置和类型化配置统一进入自定义读取 Mono。
- `src/main/java/org/jetlinks/core/device/DeviceOperator.java`、
  `src/main/java/org/jetlinks/core/Configurable.java`：删除类型化自身批量读取的 stream/collector，
  并为 0/1 key 使用空集合或 singleton 快路径。

性能环境：JDK 17.0.18、Reactor 3.7.8、G1、`-Xms512m -Xmx512m`、4 线程，
每个场景 3 秒预热、3 秒采样，基线和优化交替执行 3 轮并取中位数；全部场景
`remote_reads=0`。

| 场景 | 基线 QPS | 优化 QPS | QPS 变化 | B/op |
|---|---:|---:|---:|---:|
| `getProtocol` 设备协议命中 | 99.09M | 247.28M | +149.6% | 160 → 32 |
| `getProtocol` 产品协议回退 | 27.23M | 43.18M | +58.6% | 480 → 304 |
| `getSelfConfig(ConfigKey)` | 133.96M | 219.47M | +63.8% | 144 → 64 |
| `getSelfConfigs(ConfigKey ×1)` | 41.46M | 117.19M | +182.6% | 672 → 200 |
| `getSelfConfigs(ConfigKey ×4)` | 27.76M | 38.33M | +38.1% | 896 → 648 |
| `getConfig(ConfigKey)` 自身命中 | 67.08M | 214.95M | +220.5% | 160 → 64 |
| `getConfig(ConfigKey)` 父级回退 | 34.00M | 41.54M | +22.2% | 504 → 328 |
| `getConfigs(ConfigKey ×1)` | 78.98M | 115.85M | +46.7% | 360 → 200 |
| `getConfigs(ConfigKey ×4)` | 37.51M | 37.84M | +0.9% | 632 → 648 |

4-key `getConfigs` 原实现已经使用直接循环和 `HashSet`，没有 stream/collector 操作符可删除；
本次保持去重和调用时快照语义，没有为基准特调集合实现，因此结果视为持平。主要收益集中在
单键读取、`getSelfConfigs(ConfigKey...)` 和协议解析热路径。

JFR 结果：

- 设备协议命中路径不再出现 `MonoFlatMap` 和 `SwitchIfEmptySubscriber` 分配，采样到的响应式分配
  仅为 `MonoProtocolSupport.ProtocolSubscription`，总分配为 32 B/op。
- 产品协议回退仍包含产品解析本身的 `MonoDeviceProduct` 和配置集合分配，但原协议链的
  `MonoFlatMap` / `SwitchIfEmptySubscriber` 已消失。
- 类型化父级配置回退修复前为 520 B/op；父级直接执行类型化读取后降至 328 B/op，JFR 中
  不再出现外层 `MonoHandle` / `mapNotNull` 转换链。

验证结果：

- `MonoConfigReadTest`、`MonoConfigsReadTest`、`MonoProtocolSupportTest`：29 项全部通过。
- `mvn -DskipTests -Djacoco.skip=true package`：通过。
- 全量测试：719 项，718 通过、1 失败、2 跳过。唯一失败为既有
  `DefaultDeviceOperatorTest.testMetadataFallsBackWhenMetadataTimeCannotConvert`，原因是
  `"invalid"` 转换 `Long` 抛出 `NumberFormatException`，与本次配置和协议优化无关。
- `git diff --check`：通过。

核心配置与协议优化提交：`a3b6d601`。Pull Request：
[jetlinks-core#99](https://github.com/jetlinks/jetlinks-core/pull/99)。

## 双键与三键读取计划

目标：针对高频配置批量读取的双键、三键 L1 全命中，避免每次创建 HashMap 和节点；
提供 `Values2`、`Values3`，直接读取键值，调用 `getAllValues` 时按实际非空值数量使用
小型不可变 Map，`merge` 沿用已有 CompositeMap 语义。

影响范围：`jetlinks-core` 的 Values 实现和类型化键集合构造，以及 `jetlinks-supports` 的
`LocalCacheClusterConfigStorage#getConfigs`；不改缓存命中、缺键、过期、回源和失效逻辑，
不合并设备协议与产品协议读取，也不新增常驻设备状态。

步骤：先保留重复键兼容性并实现 2/3 键值容器；仅在两级缓存全部 L1 命中时返回轻量结果，
其它情况仍使用当前路径。类型化键集合的 2/3 个唯一非空键使用 Set.of；重复或 null 键
仍使用原 HashSet 去重。用同一环境对照 2、3 键的 QPS/分配，验证语义与并发；再以
4、6、8、12、16 键的真实调用路径评估数组型 ValuesN 是否值得引入。

风险及验证：重复键、null 值哨兵、快照、错误直接传播、异步回源、失效和并发写入均不得改变；
分阶段执行定向测试及同轮交替性能测量，最终再决定 ValuesN 上限，不用单一微基准推断收益。

### 双键、三键结果

JDK 17.0.18、Reactor 3.7.8、G1、512 MiB 堆、4 个并发工作线程；每个场景
预热 1 秒、测量 2 秒，旧实现与新实现交替执行 3 轮，取 QPS 中位数；全部 L1 命中且
`remote_reads=0`。旧实现使用本地原始 `LocalCacheClusterConfigStorage#getConfigs` 字节码，
其余模块保持相同。数值均为百万次/秒，分配为 B/op。

| 场景 | 旧 QPS | 新 QPS | 变化 | B/op |
|---|---:|---:|---:|---:|
| `getSelfConfigs(ConfigKey ×2)` | 90.74 | 160.10 | +76% | 232 → 104 |
| `getSelfConfigs(ConfigKey ×3)` | 67.10 | 90.42 | +35% | 368 → 216 |
| `getConfigs(ConfigKey ×2)` | 97.39 | 140.31 | +44% | 232 → 104 |
| `getConfigs(ConfigKey ×3)` | 58.53 | 86.93 | +49% | 368 → 216 |
| 产品标识/版本双键读取 | 107.25 | 212.99 | +99% | 200 → 104 |
| `getProtocol` 产品回退 | 31.80 | 43.56 | +37% | 304 → 192 |

`getAllValues` 在 2/3 个非空键时使用 Map.of，缺键使用空 Map 或 singletonMap；
同条件独立对照（3 轮中位数）：双键 Map 物化 82.32 → 121.97 M QPS、
360 → 248 B/op，双键 `merge` 61.95 → 88.83 M、448 → 336 B/op；
三键 Map 物化 60.06 → 67.48 M、448 → 376 B/op，三键 `merge`
49.63 → 58.40 M、536 → 464 B/op。`merge` 未引入额外操作符，收益来自小 Map。

数组型 ValuesN 候选在 4/6/8/12/16 键直接读取场景提升 QPS、降低分配；
但 `getAllValues` 与 `merge` 会重新构建 HashMap，4 键的 Map 物化
68.74 → 60.16 M QPS、合并 60.74 → 50.53 M；16 键的 Map 物化
27.06 → 20.04 M。由于通用 Values 不能预设调用方只读取单键，最终**不引入 ValuesN**，
仅为 2/3 键添加通用快路径，4 键及以上继续走现有 Map 实现。

验证：core 的 `FixedValuesTest`、`MonoConfigsReadTest`、`MonoConfigReadTest`、
`MonoProtocolSupportTest` 与 supports 的 `LocalCacheClusterConfigStorageTest` 定向通过，
core/supports 主代码编译通过。supports 完整 testCompile 被已有
`ClusterDeviceRegistryTest` 中 `Sinks.Empty#then` 的编译错误阻断；目标测试单独编译执行。
`Map.of` 对传入 Map 的 null key 查询会抛错（原 HashMap 查询返回 null），但工厂输入
的 null/重复 key 均仍走原 Map 分支；调用方如需查询 null key，应使用 `Values#getValue`。

## 元数据 Loader 常驻对象优化计划

目标：让设备及产品 Operator 自身实现 `MonoVersionedMetadata.Loader`，移除每个实例持有的匿名
Loader；不改元数据版本判断、加载链、缓存或协议回退逻辑。设备端复用 `loadSelfMetadata`，
产品端仅保存原来已构造一次的 `loadMetadata` 链，保持 `load`/`loadEmpty` 一致。

影响范围：`jetlinks-core` 的 `DefaultDeviceOperator`、`DefaultDeviceProductOperator`；
`MonoVersionedMetadata` 不变。先记录现有实现的常驻内存与元数据 QPS 基线，修改后验证冷热
元数据、版本变化、空版本和错误语义，再在同一环境比较常驻内存及协议/元数据 QPS。
产品回退订阅融合另行评估，不在本次改动中引入复杂订阅状态机。

风险：产品 `load()` 不能返回 `metadataMono`，否则发生自订阅递归；应保留既有一次构造
的加载链。构造期传递 `this` 不主动订阅；是否真正节省对象空间，以实测为准。

### Loader 实施与对照结果

`DefaultDeviceOperator` 和 `DefaultDeviceProductOperator` 已直接实现 Loader；版本转换、
快照检查、缓存命中、加载及空版本语义均与匿名实现一致。产品原加载链由局部变量改为一次构造
的实例字段；`MonoVersionedMetadata`、配置缓存及协议回退均未修改。

JDK 21.0.10、Reactor 3.7.8、G1，修改前后各自独立 JVM，交替运行三轮并取中位数：

| 10 万实例 GC 后存活堆增量 | 修改前 B/实例 | 修改后 B/实例 | 减少 |
|---|---:|---:|---:|
| 设备 Operator | 612.84 | 596.74 | 16.10 B/设备 |
| 产品 Operator | 460.71 | 436.62 | 24.09 B/产品 |

设备按既有 `DeviceApiProfile memory` 测量，产品用相同预热/GC/存活数组方式测量；
数据包含该基准构造实例所需的关联对象，不等同于单个 Java 对象头及字段大小。

| 四线程 L1 命中场景 | 修改前 M QPS | 修改后 M QPS | B/op 前→后 |
|---|---:|---:|---:|
| 产品元数据读取 | 14.32 | 14.46 | 608 → 608 |
| 设备复合元数据读取 | 12.27 | 14.16 | 664 → 664 |
| 设备协议产品回退 | 41.13 | 42.43 | 288 → 288 |

QPS 每轮预热 4 秒、测量 4 秒；`remote_reads=0`。各轮 QPS 波动大于多数前后差值，
复合元数据的中位数变化也不能据此归因于 Loader 改动；**确认的是常驻内存减少，
不声称订阅吞吐或 B/op 提升**。协议回退进一步融合订阅状态机会增加语义与维护风险，
本次不改。`MonoVersionedMetadataTest`、`MonoProtocolSupportTest` 及新增产品元数据
缓存命中/更新后重载用例共 22 项通过；core 编译与 `git diff --check` 通过。
完整测试集未在本阶段重跑，前文记载的既有错误转换用例失败仍待独立处理。

## 协议产品回退订阅优化计划

目标：在设备 `getProtocol()` 已确认需要产品回退后，复用 `MonoDeviceProduct` 的产品定位流程，
仅对同步来源省去一层产品 Mono 的订阅协调器；异步来源继续返回原有响应式查询链。
先测当前实现的 QPS 与分配，再以相同条件比较候选实现；收益不稳定则撤回候选。

范围：仅改 `jetlinks-core` 的 `MonoDeviceProduct` 和 `MonoProtocolSupport` 及其契约测试。
不改设备协议优先级、短路/错误语义、产品及配置缓存，不提前读取三键配置，不对任意
`DeviceProductOperator` 强制采用特殊路径。保持 Context、请求、取消及 Reactor Hook 的行为。

步骤：为 `MonoDeviceProduct` 提供复用原配置与产品查询的一次性内部读取入口；
`MonoProtocolSupport` 仅对未经 Hook 包装的内部实例尝试该入口，其余来源仍通过既有订阅路径。
分别验证 L1 同步命中、缺产品、空协议、异常、异步读取、Context、取消及重复订阅。

### 协议产品回退结果

`MonoDeviceProduct#lookup(Context)` 只查询本次订阅的存储、双键配置及当前产品，并返回
现有产品 Publisher；`MonoProtocolSupport#resolveFallback` 仅对内部未被 Hook 包装的
`MonoDeviceProduct` 调用它，同步来源直接读取，异步来源仍订阅响应式查询链。
缺配置、空产品、错误和设备自定义协议优先级不变。独立 `getProduct` 继续使用原 `resolve`，
不引入协议专用缓存或产品引用。曾试过让 `getProduct` 也通过 `lookup`，但对照出现
JIT 编译/分配波动，最终撤回该重构；保留的协议快路径与四线程受测字节码一致。

JDK 21.0.10、Reactor 3.7.8、G1、512 MiB 堆，当前本地 `jetlinks-supports/target/classes`
优先于测试包加载。独立 JVM 交替运行三轮，预热和测量各 4 秒，中位数如下；
全部 L1 命中，`remote_reads=0`。

| 设备 `getProtocol` 产品回退 | 修改前 M QPS | 修改后 M QPS | B/op 前→后 |
|---|---:|---:|---:|
| 4 线程 | 52.63 | 59.27（+12.6%） | 176 → 136 |
| 8 线程 | 95.11 | 108.80（+14.4%） | 176 → 152 |

QPS 受 JVM 编译和环境负载影响，各轮存在波动；可复核的主要收益是产品回退每次订阅
少一层协调对象，4/8 线程分配中位数分别减少 40/24 B。JFR 样本中同步回退的产品阶段
`StageSubscriber` 消失，剩余热点集中在协议订阅、`Values2` 和产品协议查询自身；
直接跳过设备协议检查的更快链路不满足兼容语义，不采用。

core 编译、`MonoDeviceProductTest`、`MonoProtocolSupportTest`、`MonoVersionedMetadataTest`
及产品元数据回归用例共 33 项通过，包含缺产品、同步异常、异步 Context/取消和 Reactor
调试 Hook；`git diff --check` 通过。本阶段未重跑完整测试集，前文的既有失败仍需独立处理。

## 最新评审整改计划

目标：回应 PR #99 最新评审，确认设备与产品物模型变更的一致性，消除产品及协议读取订阅器
中的 `synchronized`，并对齐 `getConfigs(String...)` 与 `getConfigs(ConfigKey<?>...)` 的多键
读取行为；保持现有 API、配置缓存、产品回退和元数据版本语义不变。

影响范围：`DefaultDeviceOperator`、`MonoDeviceProduct`、`MonoProtocolSupport`、`Configurable`
及对应测试。不修改 supports/components，不调整缓存策略，不创建通用复杂操作符框架，也不把
设备物模型空值哨兵下沉到通用 Loader 契约。

实施步骤：先补充元数据变更、多实例共享存储、多阶段取消及多键兼容测试；再分别以单个原子
状态机改造产品和协议订阅器，保证 demand、cancel、Context、error、discard/drop 及迟到信号
行为；最后为字符串双键、三键读取增加与 ConfigKey 版本一致的非空唯一键快路径，重复键或
null 继续回退现有 HashSet 语义。

风险：原子状态转换必须避免 cancel/onSubscribe、onNext/onComplete 竞态；协议多阶段切换后旧
Subscription 的迟到信号不得终止新阶段；下游在 `onNext` 中取消时不得继续 `onComplete`；
多键优化不能把原有 null/重复键兼容行为改为 `Set.of` 异常。

验证：阶段完成后统一运行定向单元测试、core 编译和 `git diff --check`；再用同环境 JMH 对比
当前 `synchronized` 基线与原子状态机的单线程、多线程吞吐及分配。若无稳定收益或引入更复杂
语义，则撤回对应无锁实现，仅保留测试和行为一致性修复。

### 最新评审整改结果

`MonoDeviceProduct` 和 `MonoProtocolSupport` 已使用单个原子状态字段发布阶段、取消和终止状态，
活动 `Subscription` 通过原子引用替换；未拆成多个互相独立的原子布尔值。产品读取保持一次活动
订阅，协议读取为每个阶段绑定明确 stage，旧阶段迟到的 `onSubscribe` 会取消、迟到的 `onError`
会 drop，不会终止新阶段。下游在 `onNext` 中取消时不再发送 `onComplete`，Context、错误映射、
discard/drop 和 demand 语义保持不变；两个订阅器均已移除 `synchronized`。

`DefaultDeviceOperator#selfMetadata()` 现在直接输出 `NON_METADATA` 哨兵，组合逻辑不再在调用点补空；
Loader 的通用 empty 契约未改变。新增两个 Operator 实例共享同一 `ConfigStorageManager` 的回归用例，
产品和设备物模型分别更新后，另一实例均按版本重新解码，避免只验证单实例本地状态。

`Configurable#getConfigs(String...)` 已与 `ConfigKey<?>...` 版本对齐：双键、三键仅在键非空且互异
时使用 `Set.of`；0/1 键、重复键和 null 键继续保持原有去重及兼容行为。测试覆盖 0/1/2/3、
双/三重复和 null 输入。

JDK 17.0.18、G1、512 MiB 堆；基线为 `0e11acc`，修改前后使用同一
`MonoDeviceReadBenchmark`，每项 2 个 fork、每 fork 预热 2×1 秒、测量 3×1 秒并启用 GC profiler：

| 标量读取场景 | 1 线程基线 M QPS | 1 线程当前 M QPS | 变化 | 8 线程基线 M QPS | 8 线程当前 M QPS | 变化 | B/op |
|---|---:|---:|---:|---:|---:|---:|---:|
| 产品读取 | 29.55 | 33.78 | +14.3% | 179.86 | 179.52 | -0.2% | 232.02 → 232.02 |
| 设备直接协议 | 54.68 | 68.39 | +25.1% | 367.81 | 450.23 | +22.4% | 104.01 → 104.01 |
| 产品协议回退 | 28.05 | 32.12 | +14.5% | 166.64 | 182.80 | +9.7% | 248.02 → 248.02 |

产品读取八线程结果在误差范围内持平，说明此场景主要受分配和共享执行资源限制，不声称多线程
提升；直接协议和产品回退在单、八线程均有稳定收益。此次无锁化不减少对象分配，收益来自移除
同一订阅生命周期内的监视器进入/退出和多阶段锁操作。

验证结果：`MonoDeviceProductTest` 9 项、`MonoProtocolSupportTest` 13 项、
`MonoConfigsReadTest` 9 项、`DefaultDeviceOperatorTest` 12 项定向通过；core 全量 738 项测试通过，
0 failure、0 error、2 skipped；主代码和测试代码编译、`git diff --check` 均通过。

## Review 5289344700 修复计划

目标：由 `MonoVersionedMetadata` 直接接收设备无独立物模型时的 `NON_METADATA` 静态 fallback，
移除 `DefaultDeviceOperator#selfMetadata()` 外层 `defaultIfEmpty`，同时保持当前“版本为空”和
“版本存在但加载结果为空”都回退到产品物模型的行为。

范围：只修改 `MonoVersionedMetadata`、`DefaultDeviceOperator`、对应单元测试和微基准；保留
`Loader#loadEmpty()` 供产品物模型执行动态初始化，不改变缓存、版本判断、解码或组合逻辑。

实现：新增带非空 fallback 的内部构造入口。版本源为空或版本转换为空时直接输出 fallback；
版本加载 Publisher 未输出值即完成时也由同一订阅状态机输出 fallback。正常缓存命中、动态
`loadEmpty()`、错误、取消、Context、discard/drop 和 demand 语义保持不变。

验证：覆盖空版本不调用动态 empty loader、版本加载为空、下游在 fallback `onNext` 中取消，
以及设备无独立物模型时仅返回产品物模型；统一运行定向测试、core 编译、`git diff --check`
和 fallback 新旧链路 JMH 对比。

### Review 5289344700 修复结果

`MonoVersionedMetadata` 新增可选非空 fallback。设备版本源为空、版本转换为空，或版本存在但
加载 Publisher 未输出值即完成时，均由现有订阅状态机直接输出 `NON_METADATA`；产品物模型
未传 fallback，仍通过 `Loader#loadEmpty()` 执行原动态初始化流程。`DefaultDeviceOperator`
移除了外层 `defaultIfEmpty`，因此每个设备少常驻一个 Reactor 包装对象，每次无独立物模型读取
也少一层订阅器；本次未单独测量 retained heap，不对具体常驻字节数作推断。

JDK 17.0.18、Reactor 3.7.8、JMH 1.35、G1，同一候选代码内对照
`MonoVersionedMetadata + defaultIfEmpty` 与内置 fallback；每项 2～3 fork、每 fork 预热
3×1 秒、测量 5×1 秒并启用 GC profiler：

| 无版本 fallback 场景 | 外层 `defaultIfEmpty` | 内置 fallback | 变化 |
|---|---:|---:|---:|
| 1 线程平均耗时 | 33.840 ns/op | 20.111 ns/op | -40.6% |
| 1 线程分配 | 144.006 B/op | 104.004 B/op | -27.8% |
| 8 线程吞吐 | 204.995 M ops/s | 335.079 M ops/s | +63.5% |
| 8 线程分配 | 152.002 B/op | 104.002 B/op | -31.6% |

该结果只覆盖无独立物模型时的操作符空分支，不代表完整 `getMetadata()` 同比提升；真实链路仍
包含产品物模型读取和组合。原始结果位于 `/private/tmp/core-pr99-fallback-{t1,t8}.json` 与
`/private/tmp/core-pr99-fallback-thrpt-t8.json`。

测试覆盖空版本绕过动态 loader、版本加载为空、fallback `onNext` 内取消，以及设备只返回产品
物模型；定向 34 项和 core 全量 742 项均通过，0 failure、0 error、2 skipped，主/测试代码编译
及 `git diff --check` 通过。

实现提交：`26b81fe8c3c8805dee09688874015add63812208`；评审入口：
`https://github.com/jetlinks/jetlinks-core/pull/99#pullrequestreview-5289344700`。

## 设备响应式 Mono 相似缺陷审查计划

目标：在 `MonoValidatedDeviceOperator` 递归订阅缺陷修复后，检查产品定位、协议回退和设备/产品
物模型读取是否存在同类缓存互相委托、同步重入、阶段切换或迟到信号导致的栈溢出、重复终止、
取消丢失及永久 pending 风险。

影响范围与 owning module：`jetlinks-core` 的 `MonoDeviceProduct`、`MonoProtocolSupport`、
`MonoVersionedMetadata`、`DefaultDeviceOperator`、`DefaultDeviceProductOperator` 及对应测试；同时复核
`jetlinks-supports` 的 `MonoValidatedDeviceOperator` 缓存缺失竞争分支和 `ClusterDeviceRegistry` 产品
缓存路径。产品缓存保存 Operator 值而不是可相互委托的 Mono，当前静态检查未发现与设备缓存相同
的 A/B 递归结构。

不做：不修改缓存策略、版本规则、协议/物模型回退语义或高频读取实现；不为任意外部自定义
Publisher 的人为自引用增加全局循环检测。只有生产可达分支能够稳定复现缺陷时才修改实现。

实施步骤：

1. 逐个核对三个自定义 Mono 的 demand、cancel、Context、同步/异步源、空值、错误、重复/迟到信号
   和阶段切换不变量，确认不存在递归追逐共享缓存值。
2. 为 supports 增加缓存首次读取为空、`putIfAbsent` 竞争返回其他实例、委托实例在 demand 前失效、
   空校验和普通缓存 Publisher 的回归场景。
3. 为 core 增加产品、协议和版本物模型的同步/异步异常、迟到 `onSubscribe`、重复/迟到信号、
   loader/fallback 空结果及并发订阅边界测试；覆盖 `DefaultDeviceOperator` 的兼容重载构造方法和
   `DefaultDeviceProductOperator` 的废弃/管理器/直接 Storage Mono 构造方法，使用受控 Publisher，
   不使用 sleep。
4. 若测试暴露真实缺陷，在 owning operator 内做最小状态机修复；否则只补行为契约测试和审查结论。

风险与验证：保持 Reactor demand、取消、Context、错误映射、discard/drop 和 assembly hook 语义；
分阶段统一运行 core 的三个自定义 Mono 与设备 Operator 测试、supports 的设备缓存与 Registry 测试，
最后执行模块编译和 `git diff --check`。不在每次测试编辑后重复全量构建。

### 审查结论与验证结果

- `MonoDeviceProduct` 的异步产品来源如果发出首值后不发送终止信号，下游会永久等待；现改为首值直接完成并取消活动上游，重复或迟到信号仍按原状态机丢弃。
- `MonoVersionedMetadata` 的 loader 存在相同问题；现将 `LOADER -> VALUE` 与首值标记合并为一次 CAS，清理并取消活动订阅后再发送值，保留下游在 `onNext` 中取消时不再收到 `onComplete` 的语义。
- `MonoProtocolSupport` 已在每个阶段首值到达后切换到下一阶段，迟到值、完成和错误不会终止当前阶段；产品缓存保存 Operator 而非相互委托的 Mono，本轮未发现与设备缓存相同的递归结构。
- 新增 `DefaultDeviceOperator` 三个构造入口以及 `DefaultDeviceProductOperator` 废弃、manager、直接 Storage Mono 三种构造入口的兼容测试，均继续走相同的产品、协议和物模型读取链路。
- 定向测试：`MonoDeviceProductTest`、`MonoProtocolSupportTest`、`MonoVersionedMetadataTest`、`DeviceOperatorCompatibilityConstructorTest`、`DefaultDeviceOperatorTest` 共 62 项通过，0 failure、0 error。
- 全量验证：`mvn test` 共 748 项，0 failure、0 error、2 skipped；`git diff --check` 通过。提交与 Pull Request：`pending`。
