
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
