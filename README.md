
# JetLinks Core
[![Maven Central](https://img.shields.io/maven-central/v/org.jetlinks/jetlinks-core.svg)](http://search.maven.org/#search%7Cga%7C1%7Cjetlinks-core)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v/https/oss.sonatype.org/content/repositories/snapshots/org/jetlinks/jetlinks-core/maven-metadata.xml.svg)](https://oss.sonatype.org/content/repositories/snapshots/org/jetlinks/jetlinks-core)
[![codecov](https://codecov.io/gh/jetlinks/jetlinks-core/branch/master/graph/badge.svg)](https://codecov.io/gh/jetlinks/jetlinks-core)

本仓库为JetLinks核心模块,提供通用的API和工具封装,
包括:
1. 设备注册中心
2. 统一设备消息定义
3. 协议包SPI定义
4. 缓存、监控
5. 集群管理
6. 事件总线
7. 通用工具类

# 响应式读取

`StorageConfigurable.getConfig/getConfigs` 在需要父级回退时使用调用级自定义 Mono 合并同步读取路径；只读取自身配置时保留 Reactor 原生单层 `flatMap` 标量快路径。异步来源和父级回退继续交由原 Reactor 链处理。`DefaultDeviceOperator` 使用 demand 驱动的 `MonoDeviceProduct` 融合设备产品解析同步热路径，首次合法 `request` 才执行读取并向活动异步源传播取消；`DefaultDeviceOperator`、`DefaultDeviceProductOperator` 使用版本化物模型 Mono 集中版本值转换、缓存检查与命中返回；外层产品/设备物模型 `zip` 仍保留原并行、错误和取消语义。

实现入口为 `src/main/java/org/jetlinks/core/config/MonoConfigRead.java`、`MonoConfigsRead.java`、`src/main/java/org/jetlinks/core/defaults/MonoDeviceProduct.java`、`MonoVersionedMetadata.java` 及原 API 调用点。不缓存配置值、Storage、产品或物模型结果，不改变缓存失效、回源和父级继承规则；自定义操作符只保存查询参数和单一策略对象，不保存跨订阅结果。版本化物模型读取会二次确认缓存对象身份，本地更新按缓存对象后版本的顺序发布，避免并发读取混用新旧快照；正常版本切换保留上游 `ON_COMPLETE`，下游取消仍传播到当前活动源。

## 执行观测

只读对象指标与注解元数据见 [对象指标与属性元数据](docs/metrics.md)，复用 `MetadataUtils`、组合注解及 `Metrics.observe`，与下方执行过程观测分别承接各自的生命周期。

Monitor 执行观测的 API、SPI、兼容边界与验证入口见 [执行观测说明](docs/monitor-observation.md)。

## 连接指标快照

`DeviceSessionInfo.of` 只读取一次连接明细，`getMetrics()` 在调用端基于收到的 `connections` 按需汇总。
汇总没有独立存储字段，并明确排除 Fastjson/Jackson 序列化；二进制字段编码也只传明细，避免重复传输或保存过时汇总。
字节和消息计数相加；`connectTime` / `latestConnectTime`、`earliestLastCommTime` / `lastCommTime`
分别保留连接建立和最近通信的时间边界。空集合或任一连接缺少快照时汇总为空，可选计数字段缺失时对应汇总为空。
汇总为弱一致快照，不保存已关闭连接的历史，不代表累计重连或故障次数；原 `connectionLifecycle` 保持可用。

确认耗时仅汇总支持确认的连接，平均值按样本数加权；P95 合并固定桶后求估计值，缺少完整分布时为空。
分布采样起点不同则不提供统一起点；多个有确认样本的快照无法判断全局最近一次确认，`ackLastDuration` 为空。
`ConnectionMetricsInfo.aggregate` 支持连接及会话分层汇总，单次遍历，不修改源快照，默认不创建直方图。
该操作只读取内存快照，不增加 I/O、后台任务、追踪 span 或 MBean。

`DeviceSessionManager.getDeviceSessionSnapshot(deviceId)` 为需要读取完整性的调用方提供 `DeviceSessionReadResult`：
会话明细、本次请求节点和失败节点分别返回，成功空会话与失败不混淆。`isComplete()` 仅在已知覆盖且所有请求节点成功时成立；
外部旧实现的默认适配标记覆盖未知。现有 `getDeviceSessionInfo` 保留原有尽力读取语义。
完整性相对于本次已发现的会话服务，不保证服务发现覆盖整个物理集群或多个节点的读取处于同一时刻；
汇总当前值的调用方应先检查完整性，并沿用当前 RPC 超时及取消机制。

只需要统计的调用方使用 `getDeviceSessionStatistics(deviceId)`：节点直接从当前连接读取一次指标，
返回会话数、可用的连接数、会话时间及连接指标，不生成或传输连接身份与地址。
`DeviceSessionStatistics` 使用版本化 `Externalizable`，以位标记省略空值、零值和空直方图桶；
跨节点合并保留确认样本数、总耗时和桶计数，不平均节点的平均值或P95。
请求端报告实际节点覆盖；空节点计数0，缺失能力保持未知，旧管理器默认适配保留原有覆盖语义。
旧节点不支持统计RPC时应报告读取失败，不能静默返回0或把覆盖提升为完整。

# Hex 点位编解码

内置 `hex` codec 将全部可读字节转换成字符串，例如 `00 01 41 A0` 解码为
`"000141a0"`，保留前导零，采用小写且不带 `0x` 前缀或分隔符。长度为动态长度，
编码时接受偶数位的大小写十六进制字符串并还原字节；非法字符或奇数位输入会被拒绝。

`hex` 与其他点位 codec 一样遵循配置的字节布局：读取时先调整布局，再转换成 Hex；
写入时先将 Hex 还原成字节，再调整布局。例如原数据为 `000141a0`、布局为 `CD_AB` 时，
点位值为 `"41a00001"`；写入 `"41a00001"` 时，下发字节为 `00 01 41 A0`。
页面“原数据”保留采集字节顺序，点位解析值以字符串沿既有上报和历史存储链路处理；
关联设备属性时应使用字符串类型。

点位写入时，编码结果短于固定字节布局的宽度，先在末尾补零再调整布局，保证读取和
写入使用相同的字节位置。例如 BCD8 的 `23` 编码为 `23`，在 `AB` 布局下写入 `23 00`，
在 `BA` 布局下写入 `00 23`，两种布局均可读回 `23`。

# ASCII 点位编解码

内置 `ascii` codec 使用 US-ASCII，解析值为字符串，支持动态字节长度。读取时消费全部
可读字节，以首个零字节为字符串结束符，例如 `41 42 43 00 00` 解码为 `"ABC"`，
`41 00 42` 解码为 `"A"`；保留结束符之前的空格，忽略结束符之后的填充。
正文中超过 `0x7F` 的字节及中文等非 ASCII 字符会被拒绝。

编码只输出字符串对应的 ASCII 字节，例如 `"ABC"` 编码为 `41 42 43`；
文本包含 NUL 时拒绝写入，避免值被截断或导致存储失败。
协议头、固定宽度补齐和字节布局调整由调用方负责；解码校验失败不推进缓冲区读索引，
编码校验失败不写入部分数据，缓冲区由调用方释放。

# UInt64 点位编解码

内置 `unsigned_int64` codec 每个值占 8 字节，使用 `BigInteger` 表示
`0` 到 `18446744073709551615`，避免最高位为 1 时被解析为负数。
编码拒绝负数和大于 `2^64-1` 的值，解码要求至少有 8 个可读字节。
`unsigned_int64_array` 支持连续的 8 字节无符号整数；字节布局由调用方调整。

# License
Apache-2.0

# 相关链接
[JetLinks 社区版](https://github.com/jetlinks/jetlinks-community)
[hsweb企业级后台管理框架](https://github.com/hs-web/hsweb-framework)
