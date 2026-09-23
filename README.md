
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

# License
Apache-2.0

# 相关链接
[JetLinks 社区版](https://github.com/jetlinks/jetlinks-community)
[hsweb企业级后台管理框架](https://github.com/hs-web/hsweb-framework)
