# ObjectOperations SPI 设计

状态：已实现首期通用 SPI、relation 适配和后端接口。本文维护对象操作及语义契约；Monitor 采集与传播、本体业务模板继续在各自原文档维护。具体来源的支持范围与集中验证见第 9 节。

实现保留“通用物模型 + 一个实例操作 SPI + 原关系/命令能力”。收敛事项见 7.2，官方文献对照见 7.3，契约见 7.4，按需求扩展的边界见 7.6。未将所有业务来源一次性迁移；旧来源只暴露原先具备的能力。

## 1. 目标与模块边界

逻辑来源选择使用 `ObjectOperations.forProvider(providerId)`，所选句柄的
`getProviderId()` 与批量 `ObjectProperty.getProviderId()` 保留真实提供方。
未限定或旧来源返回 null，不推断提供方。默认方法保持既有实现可用，
不改变 SimpleObjectProperty 的已发布 Externalizable 格式。
提供方发现、注册和多来源组合归 relation，core 不依赖业务或本体。

以 `type + id` 标识对象，以物模型描述属性、功能、事件，以关系模型描述对象间的联系。观测、事件发生、活动执行、结果保留原数据结构，通过元数据和关系表达共同语义，不另建万能数据 DTO 或查询函数注册中心。

| Owning module | 责任 |
| --- | --- |
| jetlinks-core | 对象操作、通用物模型及语义扩展；复用 ThingMetadata、CommandSupport 及关系接口 |
| relation-component | 在既有 Provider/Manager 上提供适配、发现、导航和集合查询；复用平台关系存储与集群路由 |
| 设备、告警、通知等来源业务 | 实现原数据读取、命令执行、来源关系、权限及状态校验，不依赖 ontology |
| ontology | 组合业务模板、路径、事实和证据，解释完成条件；RelationFact 保留在其内部 |

不新增对象注册中心、Thing 生命周期、存储引擎、流程或推理引擎、查询语言。取得句柄不代表对象存在或已获授权，声明语义也不等于已经保存完整血缘。

## 2. 包结构与实例 SPI

```text
jetlinks-core/.../org/jetlinks/core/
├── metadata/
│   ├── Metadata.java               复用：expands/getExpand(ConfigKey)
│   ├── MetadataSemantics.java      新增：通用语义键及定向读取辅助方法
│   ├── SemanticType.java           新增：模型所描述数据的语义角色
│   ├── PropertySemanticRole.java   新增：字段含义
│   └── FunctionKind.java           新增：只读功能/动作分类
├── things/
│   ├── ThingMetadata.java          优化：复用通用模型，补可选关系目录
│   ├── ObjectOperations.java       新增：实例操作契约
│   └── relation/
│       ├── RelationSemantic.java   新增：关系语义
│       ├── RelationObject.java     修改：继承 ObjectOperations
│       ├── PropertyOperation.java  复用
│       ├── RelationOperation.java  复用
│       ├── ObjectType.java         保留 Metadata 分类及已发布关系 SPI
│       └── Relation.java           复用
└── command/CommandSupport.java     复用，不扩大接口

relation-component/.../org/jetlinks/pro/relation/
├── RelationObjectProvider.java     修改：增加 operations(id)
├── RelationObjectManager.java      修改：完整模型发现、关联分页便利入口
├── RelationObjectProviders.java    复用：唯一身份与独立能力来源路由
├── ObjectSupportProvider.java      关系层：同一对象的补充模型与操作
├── query/RelatedObjectQuery.java   新增：关联查询请求，贯穿 Manager/Provider
├── metadata/ObjectDataMetadata.java 新增：数据行的 $metadata 值类型
├── metadata/ObjectLink.java        新增：数据行相对另一对象的已知联系
├── impl/ObjectData.java            优化：保留 Map，增加 $metadata 类型化访问
└── impl/                          旧接口/模型适配、对象委托、集群代理
```

core 的实例运行时 SPI 保持 ObjectOperations；模型统一复用 ThingMetadata，不增加组合模型接口。
relation 的 ObjectSupportProvider 声明补充来源，复用现有 Provider 注册能力；不增加 Factory 或独立注册框架。
数据元信息值类型、枚举和辅助工具不承担运行时调度。默认适配器、只读模型视图和不支持句柄为内部实现。

### 2.1 ObjectOperations

```java
public interface ObjectOperations extends Wrapper {

    String getType();

    String getId();

    default String getProviderId() { return null; }

    default ObjectOperations forProvider(String providerId) { throw new UnsupportedOperationException(); }

    Mono<? extends ThingMetadata> getMetadata();

    PropertyOperation properties();

    RelationOperation relations(boolean reverse);

    default RelationOperation relations() {
        return relations(false);
    }

    CommandSupport commands();
}
```

| 方法 | 调用与结果契约 |
| --- | --- |
| getType/getId | 构造时校验非空，句柄存续期间身份不变 |
| getProviderId/forProvider | 可选逻辑来源标识及显式来源句柄；不改变对象身份，也不授予权限 |
| getMetadata | 订阅时解析实例适用模型；空表示旧来源没有完整模型，不能解释为实例不存在；解析及服务错误保留 |
| properties | 同步返回非空句柄，无 I/O；get 在订阅时读取当前值，没有值沿用原空结果，不伪造 null、0 或空字符串 |
| relations(reverse) | 同步返回非空句柄；false 导航出边，true 导航入边；返回另一端对象，实际读取在订阅时执行 |
| commands | 同步返回非空 CommandSupport；入口无 I/O，以响应式命令在订阅时执行原业务校验和操作 |

句柄只保存身份及原业务依赖，不保存认证、Recorder、业务快照或一次调用的解析结果，可并发复用。每次订阅读取当前认证及 Monitor 上下文；不擅自 subscribe、阻塞、改变调度器或自动重试动作。取消、超时及背压沿用原链路。

commands() 不返回 Mono；异步解析服务在内部使用既有 AsyncProxyCommandSupport，并以 defer 保留每次订阅上下文。调用者使用 executeToMono/executeToFlux 或原明确类型的响应式命令，不改变其他 CommandSupport 调用方的既有执行契约。

无命令时提供共享、不可变的不支持句柄：目录为空，创建或执行沿用 `error.unsupported_command` 明确失败，不能空完成冒充成功。新 ObjectOperations 实现显式提供操作句柄；没有关系读取能力同样不能以空数据冒充完整查询。

现有 RelationObject 继承 ObjectOperations，保留原方法签名，仅对新增 getMetadata/commands 提供兼容默认值（空模型/不支持句柄）。DefaultRelationObject 委托来源句柄提供新能力；DefaultRelatedObject 通过同一基类继承，防止导航后丢失模型和命令。

沿用 Thing 的“模型 + 实例操作”，不继承其 Configurable、注册/注销及模板生命周期。属性写入仍走原业务命令，不增加 PropertyOperation.set；Wrapper 只用于本地已知扩展解包，不作为远程能力协议。

### 2.2 直接增强通用物模型

物模型解析与复制契约：根空输入表示空模型，重载时清空旧身份、扩展及所有目录；
目录缺失或为 `null` 表示没有定义，但目录条目必须为对象。
core 的 `metadata/MetadataDefinitionParser` 以 JDK `Map` / `Collection` 统一校验对象与目录结构，
非法目录、空条目、标量、数组条目及非字符串键均返回 `error.invalid_metadata_definition` 格式错误；
不将字符串或 Bean 隐式转换为物模型定义。属性、功能输入、事件、标签、关系、递归模块及对象类型属性
复用此边界，重复标识沿用第一个定义。解析器返回独立容器，不修改来源容器，不依赖 JSON 引擎。
`Jsonable` 保持原公开契约，具体模型仅在调用旧 `JSONObject` 签名时适配 Map；
新增解析不使用 Fastjson 的文本解析、对象转换或数组访问方法，不新增 JSON 引擎依赖。
`SimpleDeviceMetadata.fromJson` 在替换旧状态前完整解析属性、功能、事件、标签、关系与模块；
功能和事件通过 `SimpleFunctionMetadata`、`SimpleEventMetadata` 及原 `DataTypes` 注册表进行 JSON 往返，
不依赖 supports 实现、不阻塞或加载业务来源。
supports 的 `DefaultThingsMetadata` 在复制和合并时隔离关系声明与递归模块对象，各层扩展 Map 独立，
嵌套扩展值沿用原值语义。沿用原 JSON 字段和可选目录形状，不调整业务接口或执行链路，也不新增追踪或 MBean。

可选类型与复制边界：事件缺失、null 或未注册类型沿用未知类型语义，默认 Codec 往返后仍可安全访问；
数组元素和枚举值的显式类型通过 supports 的 `JetLinksDataTypeCodecs` 共用解析边界，
保留类型名简写及对象定义，未声明的嵌套类型仍可选，显式空对象或未注册类型返回 i18n 格式错误。
属性未声明类型仍返回 null，字符串描述以 unknown 表示；`SimpleFunctionMetadata.copy` 通过
非空 `getInputs()` 取得参数并复制容器，无参数功能可直接复制。回归用例覆盖事件默认类型经默认 Codec 往返、
数组/枚举的类型名与对象定义、无效类型的错误边界、可选类型空值和无参功能复制容器隔离。

空产品模型参与组合和合并时仍是空目录，不应丢失另一侧的设备独立定义；两侧都为空时可安全编码和再次解析。
空输入还包括全目录为 null 或空列表的模型；平台返回的空 `SimpleDeviceMetadata` 与默认 Codec 模型可相互组合，
`ignoreExists` 合并及产品优先的组合均保留原重复标识定义，同时追加新标识。回归矩阵覆盖上述表示与六类目录。

验证入口（聚合工程）：
`mvn -o -B -ntp -pl jetlinks-core,jetlinks-supports -am -Djacoco.skip=false -Djacoco.append=false -Dtest=MetadataDefinitionParserTest,SimpleDeviceMetadataTest,MetadataEmptyModelTest,MetadataNpeRegressionTest,MetadataSemanticsTest,MetadataDirectoryTest,MetadataExpandTest,PropertyMetadataTest,CommandMetadataResolverTest,JavaBeanCommandSupportTest,MetadataUtilsTest,MergeOptionTest,PresentationAnnotationsTest,ExpandsMergeTest,ExpandsMergesTest -Dsurefire.failIfNoSpecifiedTests=false test`。
验证（2026-11-11）：新增回归在修复前复现事件类型、数组元素类型、无参功能复制及属性字符串描述的 NPE，
日志 `/private/tmp/jetlinks-metadata-npe-red-20261111.log`。修复后主代码与测试代码编译成功，
core 70 项、supports 39 项，共 109 项通过，无失败、错误或跳过；
日志 `/private/tmp/jetlinks-metadata-empty-coverage-final-20261111.log`。
平台公开方法矩阵另有 220 个用例通过（207 个边界矩阵用例、13 个已有回归），覆盖产品解析、设备详情的产品/派生模型组合、
设备模型合并及产品/设备缓存读取；日志 `/private/tmp/platform-empty-metadata-20261111.log`。
该矩阵使用平台已编译类和修复后的 SDK 类，不修改平台源码，不代表完整平台构建或 HTTP/数据库保存链路验证。
本地复核入口为 `/private/tmp/run_platform_empty_metadata.py` 与 `/private/tmp/PlatformEmptyMetadataTest.java`。
合并 SDK 与平台执行数据后，JaCoCo 在两个 PR 新增/修改的可执行行上覆盖 core 101/101、supports 61/61，
这些行上的分支覆盖分别为 24/24、26/26；这是改动范围覆盖率，不是整库覆盖率。
报告 `/private/tmp/jetlinks-metadata-combined-coverage-20261111/index.html`，统计入口 `/private/tmp/summarize_metadata_coverage.py`。
新增用例在 `jetlinks-core/src/test/java/org/jetlinks/core/metadata/MetadataDefinitionParserTest.java`、
`jetlinks-core/src/test/java/org/jetlinks/core/metadata/SimpleDeviceMetadataTest.java` 与
`jetlinks-supports/src/test/java/org/jetlinks/supports/official/MetadataDirectoryTest.java`、`MetadataNpeRegressionTest.java`、`MetadataEmptyModelTest.java`，覆盖根空输入、重载清空、
非法目录、条目及嵌套对象/功能输入、普通 Map/List、重复标识、非法输入不替换旧模型、双向修改隔离与默认 Codec 往返。
本次未发布或替换部署制品，运行环境仍需更新包含修复的 core/supports 并重启。
交付为 core [PR #112](https://github.com/jetlinks/jetlinks-core/pull/112) 与
supports [PR #51](https://github.com/jetlinks/jetlinks-supports/pull/51)；初始修复提交分别为 `8a10b5b5`、`e905964`，
结构解析修订维护在同一组 PR。supports 配套依赖 core 修复，按 core、supports 顺序合入和发布。

语义是模型的通用描述，不属于某种对象操作门面。设备、业务记录、事件载荷和命令输出可以复用同一组语义约定；它们通过原模型及其 expands 声明，不要求先转为另一套模型。物模型只描述结构和含义，不因此要求实现 Thing 生命周期。

| 声明位置 | 内容 |
| --- | --- |
| ThingMetadata.expands | 模型所描述数据的 semanticTypes，例如 resource、observation、event |
| PropertyMetadata.expands | 原字段的 semanticRole；单位、枚举及值结构继续使用原 DataType |
| FunctionMetadata.expands | functionKind=query/action，输入输出保留原模型 |
| Relation.expands | 原关系 ID 的 semantic，保留规范方向 |

语义键及类型化读取集中在 core.metadata.MetadataSemantics；不向 Metadata 基础接口堆叠各类语义 getter，也不再维护一份语义状态。

Java 声明复用 `MetadataUtils` 的组合注解解析：`@SemanticRole` 声明字段角色，`@SemanticTypes` 声明模型角色，`@FunctionSemantics` 在命令类或处理方法上显式声明 query/action；未声明的功能保持未知。枚举值映射为标准字符串或数组，未知扩展词仍可通过原 `@Expands/@Attr` 使用。`@GraphSubject` 只标记结果所属对象，不隐含记录 identity；记录自身 ID 应独立声明。带语义注解的字段仍需非隐藏 `@Schema`，不能通过展示注解意外公开私有字段。

`@Presentation(primary=true, observedAt="sampleTime")` 将主值绑定到实际采样时间字段，允许多主值；不使用查询时间代替采样时间。`observedAt` 支持最多 8 段的对象路径，目标必须是 date/long。可选空值通过 `@Attr(ignoreEmpty=true)` 省略，false/0 保留；不增加旧字段的空默认键。`MetadataSemantics.validateOutput` 拒绝多主体、无效时间路径和已知语义冲突；相同声明可合并。原物模型 JSON/codec/RPC 继续承载这些元数据，不建立平行扫描器或传输模型。

语义合并按实际含义比较：semanticTypes 的注解数组、JSON 集合、顺序与重复词项不构成冲突，未知合法词仍保留；不同角色和 query/action 声明继续拒绝。命令类、显式引用命令类与处理方法经同一合并规则，普通方法选项仍保持原优先级。采样时间路径相对字段所属对象，嵌套对象与多个主值分别解析；规范化图谱事实保留真实时间，缺失或非法源值保持未知，视图可显式选择其他已声明时间字段。

`core.metadata.ExpandsMerge` 提供不可变的字段规则：`KEEP_FIRST`、`OVERWRITE`、`requireEqual`、自定义同步值函数 `of` 和显式 `object` 子键合并。`ExpandsMerges.defaultStrategy()` 提供统一默认入口，通过 `ExpandsMergeProvider` SPI 发现定义方规则；`MetadataSemantics.MergeProvider` 只声明所属语义规则。`MetadataUtils` 只解释默认快照，不引用语义专用策略常量或识别扩展名。普通值仍先声明优先，未配置规则的同名注解命名空间仍整体覆盖；对象合并复制目标子对象，不修改来源。`primary/observedAt` 的严格规则只作用于 `presentation` 内，不影响其他命名空间的同名普通选项。

应用通过 `ExpandsMerges.register(key, rule)` 或 `register(rules)` 注册完整顶层键规则；输入会被复制，后注册的同名键覆盖先注册。返回的 `Disposable` 由注册方释放，重复或乱序注销只撤销该次注册，恢复其余有效注册或 SPI 默认规则。对象子键不会与被覆盖策略隐式拼接；SPI 同名键重复属于装配错误，避免把类路径枚举顺序当作优先级。

```java
Disposable registration = ExpandsMerges.register("card", ExpandsMerge.object(Map.of(
    "caption", ExpandsMerge.OVERWRITE)));
// 此后 MetadataUtils 的默认解析与合并均使用该规则。
// 组件卸载时调用 registration.dispose()，释放此注册。
```

默认策略按类加载器共享，只保存不可变规则，不保存请求或认证。注册/注销发布一次完整快照，读取无锁；一次注解解析/合并及同一 MetadataParser 使用同一快照。规则变更只影响后续解析，不追溯刷新已构造的模型或 CommandSupport。自定义扩展仍可向 `MetadataUtils.parseExpands(element, rules)`、`parseExpands(rules, annotations)` 和 `mergeExpands(target, additions, rules)` 提供显式局部规则，局部规则不读取或修改默认策略。`requireEqual` 接受定义方比较器及异常供应者；`unorderedEquals` 仅用于明确无序的数组/集合，普通值不做集合化。未知词、原值及 JSON/RPC 结构保持；原 MergeOption 的显式忽略/删除合同保留。没有 I/O、后台任务、模型缓存刷新或独立追踪/MBean。

合并规则验收（core [#103](https://github.com/jetlinks/jetlinks-core/pull/103)、配套 supports [#47](https://github.com/jetlinks/jetlinks-supports/pull/47)）：在各仓库根分别执行 `mvn -o -B -ntp -Dtest=ExpandsMergesTest,ExpandsMergeTest,PresentationAnnotationsTest,MetadataSemanticsTest,CommandMetadataResolverTest,MetadataUtilsTest,MergeOptionTest install` 和 `mvn -o -B -ntp -Dtest=JavaBeanCommandSupportTest,MetadataDirectoryTest install`，48 + 19 项通过，无失败、错误或跳过，两个制品已本地安装。最终 core 包含 5 项注册用例，覆盖自定义同步值函数、SPI 覆盖恢复、最新及乱序注销断言，重复执行不累计。覆盖自动 SPI 发现、注册规则经注解/字段/命令适配生效、局部规则隔离、并发快照、普通值/数组原顺序、语义等价与冲突，以及原 codec 目录往返。日志为 `/private/tmp/ontology-global-merge-{core-final,supports}.log`。未变更的观测、JMH、本体及 UI 证据沿用，未重打包或管理运行服务，混合版本与生产容量未新增验收。

关系定义作为通用模型的可选目录，由独立 RelationMetadata 描述目标类型、关系标识、方向、名称及语义。使用 getRelationMetadata 避免与已发布 ObjectType.getRelations() 的 Map 接口冲突，原关系 SPI 不承担 JSON 解析：

```java
public interface ThingMetadata extends Metadata, Jsonable {

    default List<RelationMetadata> getRelationMetadata() {
        return Collections.emptyList();
    }

    default List<RelationMetadata> getRelationMetadata(String type) {
        return getRelationMetadata().stream()
            .filter(relation -> Objects.equals(type, relation.getTargetType()))
            .collect(Collectors.toList());
    }
}
```

- ObjectType 保留 Metadata 分类、属性及原关系方法，不继承 ThingMetadata。完整模型通过 RelationObjectProvider.getMetadata() 独立取得，实例适用模型通过 ObjectOperations.getMetadata() 取得。旧 Provider 默认适配原分类、属性和关系，未声明功能/事件保持为空。
- 复用 ThingMetadata 描述完整模型，与 ObjectType 的关系管理职责通过适配分离。原设备/产品模型不自动成为关系中心的对象类型，也不要求实现 getRelatedTypes；关系目标仅引用 type，不递归加载其完整模型。
- ThingMetadata.getId() 保留原模型身份，可以是产品/模板 ID；ObjectOperations 的 getType/getId 仍为对象类型/实例身份。类型发现返回的 ObjectType.getId() 仍等于 Provider.getTypeId()，不能将设备产品模型改名为 device 或用产品 ID 替代对象类型。
- 属性、功能、事件、标签和模块继续复用 ThingMetadata，集合在新适配视图中非 null，按 ID 查找遵循原契约。完整模型的语义通过 MetadataSemantics 读取；实例适用模型按次形成只读快照，不修改原共享物模型。
- 类型模型给出稳定结构，实例模型解析产品/模板后补齐真实结构；适配层按权威定义补齐适用关系。不能任取一台设备补齐类型目录，也不自动把当前实例的关系写回共享产品模型。
- 功能来自实际处理器的 FunctionMetadata，保留输入、输出 DataType 及流式结果，公开功能 ID 与命令 ID 一致。状态及权限仍在调用时校验，不能仅复制模型就宣称可执行或可订阅。

仅增加默认方法不能完成关系能力：原 SimpleDeviceMetadata、CompositeThingMetadata、CompositeObjectType、物模型 Codec 和集群投影都要验证关系及语义不丢失。旧模型缺省仍为空；语义含义变化、同 ID 冲突和版本策略必须显式校验，不能只把未知字段传过去就宣称来源支持。扩展后的模型 JSON 应同时保留原 properties/functions/events/tags/expands 及可选 relations，不复制一层对象模型。

语义注解和可选关系目录通过统一物模型表达；ObjectType 旧接口与独立模型获取分别验证。旧来源的默认适配不证明其已经获得新能力；旧接口的编码形状保持，不能无条件给全部物模型响应追加新字段。

## 3. 语义词汇与元数据

### 3.1 最小枚举

```java
// org.jetlinks.core.metadata
public enum SemanticType {
    resource, observation, event, activity, result
}

public enum PropertySemanticRole {
    identity, observedProperty, result, quality,
    observedAt, occurredAt, startedAt, endedAt, recordedAt, status
}

public enum FunctionKind {
    query, action
}

// org.jetlinks.core.things.relation
public enum RelationSemantic {
    structural, about, used, generated, triggered
}
```

角色允许重叠：指标记录可同时为 observation/result，告警发生可同时为 event/result。它们不是互斥 OWL 类；业务类型仍有自己的 typeId，不为每种告警、短信重试扩充枚举。

resource 表示独立业务主体。有稳定 ID 的观测、事件和执行也可以被引用，无需全部成为一级管理资源或登记为 Thing；具有独立办理生命周期的告警单、工单可以按业务需要声明 resource。

### 3.2 MetadataSemantics

扩展复用原 expands，线缆使用字符串/字符串数组，避免序列化 Java 枚举类名：

```java
public static final ConfigKey<String[]> SEMANTIC_TYPES =
    ConfigKey.of("semanticTypes", "semanticTypes", String[].class);

public static final ConfigKey<String> PROPERTY_ROLE =
    ConfigKey.of("semanticRole", "semanticRole", String.class);

public static final ConfigKey<String> FUNCTION_KIND =
    ConfigKey.of("functionKind", "functionKind", String.class);

public static final ConfigKey<String> RELATION_SEMANTIC =
    ConfigKey.of("semantic", "semantic", String.class);
```

工具只提供以下定向解析方法，不解析路径表达式或调用业务：

```java
static Set<SemanticType> semanticTypes(Metadata metadata);
static Optional<PropertySemanticRole> propertyRole(PropertyMetadata property);
static Optional<FunctionKind> functionKind(FunctionMetadata function);
static Optional<RelationSemantic> relationSemantic(Relation relation);
```

MetadataSemantics 是无状态工具，常量和解析共同维护一套扩展键，不注册模型或执行操作。Metadata 已有 getExpand(ConfigKey)，直接复用；Relation 只有 getExpands()，由辅助方法安全读取，不能假设它也实现 Metadata。解析结果不可变；来源声明校验拒绝错误形状，消费未知新词时保留原扩展数据，返回已识别角色或 Optional.empty，不擅自降为 structural/query。

枚举只表示首期内置词汇，线缆词汇保持开放。semanticTypes、semanticRole 和 semantic 分别说明数据角色、字段角色和关系含义；需要引用外部领域概念时可按 7.6 的扩展约定处理，不把模型字段名或展示名称当作语义标识。声明中的名称可以国际化，类型 ID、关系 ID、语义词和扩展键不随语言变化。

ontology 现有 RelationDefinition.Semantic 的词汇迁移为引用 core 的 RelationSemantic；若需要兼容已发布类型则保留显式过渡转换，不长期维护两套含义，也不改变既有 expands.semantic 的字符串值。

### 3.3 字段与功能

| semanticRole | 原字段代表什么 |
| --- | --- |
| identity | 来源稳定记录 ID，不是目标设备 ID、时间戳或定义 ID |
| observedProperty | 被测量/计算的属性标识，例如 temperature |
| result | 观测值、计算或业务结果，保留原 DataType，可为结构值 |
| quality | 原数据质量状态，保留原枚举及含义 |
| observedAt / occurredAt | 观测时间 / 事件发生时间 |
| startedAt / endedAt | 活动实际开始 / 结束时间 |
| recordedAt | 记录保存时间，不冒充观测或发生时间 |
| status | 来源状态，保留原状态机 |

原字段 ID、单位、枚举及结构不改名。一字段首期声明一个 semanticRole；多个字段同为时间/结果角色时由调用者显式选择，不任取第一个。计算窗口、等级、定义版本继续用原属性模型表达，出现真实通用消费需求再补词汇。

functionKind=query 表示只读计算或专用读取；action 表示会产生控制、发送、重试等业务副作用。它不同于 isAsync 或 Mono/Flux 返回形状。未分类保持未知，大模型工具不能自动当作只读功能执行；分类不替代授权和状态校验。

### 3.4 关系与方向

关系 ID 保留原名称，semantic 只补含义，例如告警已有 target 可声明为 about，不必改关系 ID。

| semantic | 规范方向 | 事实含义 |
| --- | --- | --- |
| structural | 原定义 source → target | 位于、包含、所属等结构联系 |
| about | 记录/结果 → 主体 | 这条数据关于谁，允许多个主体 |
| used | 活动 → 实际输入 | 此次执行实际使用的记录/对象 |
| generated | 活动 → 实际输出 | 此次执行实际生成的结果 |
| triggered | 触发事件/活动 → 后续活动 | 有明确执行关联的触发 |

不凭关系 ID、时间邻近、alarm target 或 notification origin 推断因果。反向导航仍是同一条规范关系，不生成另一条边。定义只声明一次；主体模型可投影入边便于发现，此时 Relation.isReverse() 为 true，保留原 ID/semantic，查询时不重复反转。

规范关系按来源类型、关系 ID、目标类型共同定位；relationId 不是跨类型全局唯一键。相同 ID 的不同关系不能按名称合并，入边投影也不能改变原定义归属。

语义借鉴 [SOSA/SSN](https://www.w3.org/TR/vocab-ssn/)、[PROV-O](https://www.w3.org/TR/prov-o/)、[OWL 2 Primer](https://www.w3.org/TR/owl2-primer/)；交互沿用物模型及 [WoT TD](https://www.w3.org/TR/wot-thing-description11/) 思路。本方案不宣称实现这些标准的完整序列化、推理体系或同名谓词等价映射。

## 4. Provider 接入与兼容

在既有 RelationObjectProvider 上新增方法，其他入口保留：

```java
default ObjectOperations operations(String id) {
    return new LegacyRelationObjectOperations(this, id);
}
```

LegacyRelationObjectOperations 是内部兼容适配器。新 Provider 覆盖 operations(id)，同步返回捕获身份和原业务依赖的轻量句柄，不执行 I/O；返回 type/id 必须与 Provider 和参数一致，由管理器检查。Provider 不提供 commandBindings、服务路由表或参数表达式。

- getType() 继续返回 Mono<ObjectType>，用于分类、原权限和关系管理；getMetadata() 返回 Mono<ThingMetadata>，用于独立模型发现，不依赖虚构实例。远端分别传输旧类型声明和完整物模型，不把完整模型写回 SimpleObjectType。
- 旧 properties/metadata/集合查询及固定关系入口保留；新来源的旧属性入口可转发到自己的 operations(id).properties()。
- 默认适配器只调用旧入口，不能把全部旧入口默认反向转发到 operations，避免递归。
- 旧模型只适配已提供结构，功能/事件为空、语义未知，不根据类型名补造能力。ObjectType 与 Provider.metadata() 可能不同，适配明确字段来源，不静默覆盖冲突。
- 关联分页新增 `doQueryByRelatedPage(RelatedObjectQuery request)` 重载，与 Manager 使用同一请求契约，结果复用 PagerResult<ObjectData>。旧多参数方法无法统一保证返回端筛选、授权与计数，默认实现明确不支持，不猜测它是否已实现。来源覆盖新重载，按原业务依据提供查询并明确处理 at；旧方法不反向转发。Manager 按可信定义补齐行元信息。

```text
RelationManager.getObject(type, id)
  → 既有 Provider 路由 → DefaultRelationObject
      → properties/commands/getMetadata：委托 provider.operations(id)
      → relations：复用固定来源关系 + 平台管理关系的既有路由

旧 Provider.operations(id) → 兼容适配器 → 旧属性/模型/固定关系入口
新 Provider.operations(id) → 自有轻量句柄 → 原 API 或既有命令代理
```

固定关系的导航与分页必须读取同一原业务依据；来源 relations() 可包装该固定逻辑，不能再与 Provider 查询重复执行并无条件 merge。平台关系继续由原 relation 服务维护，来源不注入聚合后的 RelationManager，不自建动态关系存储。RelatedObject 以另一端为自身身份，保留与本次起点的关系信息。

DefaultRelationOperation.save 当前未实现。首期统一门面只承诺读取/导航，未实现写入应明确不支持，不能继续空完成；用户关联/解绑复用原管理服务，来源所属关系由原业务维护。

## 5. 类型发现及关联分页

### 5.1 独立请求对象

关联查询使用 `org.jetlinks.pro.relation.query.RelatedObjectQuery`，不要把 DTO 放在 web 包，也不继承方向含义不同的旧 ObjectQueryParam。采用普通可序列化 Bean，省略访问器、校验注解和真实模块版本注释：

```java
public class RelatedObjectQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private ThingId subject;

    private String relatedType;

    private String relationId;

    private boolean reverse;

    private QueryParamEntity query = new QueryParamEntity();

    // 可空，关系有效时点，Unix 毫秒；不是目标记录的发生时间过滤。
    private Long at;
}
```

首期字段对应已有业务需求，不增加任意 options/expands 参数袋。后续字段须有明确语义、缺省行为及来源支持策略，不能靠 Provider 忽略未知选项维持表面兼容。不要把用户身份、租户、Recorder 或绕过权限的开关放入请求；这些仍来自可信上下文。

at 与 ontology 现有 RelationQuery.at 使用相同的毫秒有效时点语义：null 查询当前关系；非 null 必须由来源按真实有效期/快照查询。它不要求读取目标对象所有属性的历史快照，也不提供跨来源的一致性快照。来源没有历史、旧节点不支持该字段时明确失败，不能退回当前关系；告警发生时间、观测窗口仍用 query 筛选目标记录。

RelatedEntity 当前只有 createTime，没有完整有效期及解绑历史；不能拿 createTime 冒充关系历史。此请求为来源已有历史能力提供传参契约，不要求首期给所有关系存储增加历史系统。

### 5.2 Manager 与 Provider 贯通

在既有 RelationObjectManager 增加两个便利方法，不新增 Manager 或注册中心：

```java
Mono<ThingMetadata> getMetadata(String type);

Mono<PagerResult<ObjectData>> queryRelatedPage(RelatedObjectQuery request);
```

Provider 增加同请求对象的重载，保留原多参数方法；以下为重载签名，兼容默认适配在实现阶段补齐：

```java
Mono<PagerResult<ObjectData>> doQueryByRelatedPage(RelatedObjectQuery request);
```

getMetadata(type) 用于未选实例时发现完整类型模型，复用 Provider.getType() 与只读适配，避免用伪 ID 调用实例入口。原 metadata(type) 保留为集合字段目录。新增方法对旧 Manager 实现提供明确不支持的兼容默认值，由原默认实现/集群代理具体支持。QueryParamEntity、PagerResult、ObjectData 留在 relation 组件，不下沉 core。

Manager 校验并快照化请求，先解析规范关系及实例归属，再选择权威读取路径。Provider 所属关系按规范出边归属路由：reverse=false 为 subject.type，reverse=true 为 relatedType；平台管理关系读取既有关系服务，不能全部交给 Provider 的固定关系方法。请求传到远端时保留同一语义，返回记录仍始终是 relatedType。旧方法的参数映射另外用双向契约测试验证，不能将路由方向与返回类型混为一谈。

平台管理关系的边分页与目标对象分页并不等价。来源不能把固定主体、目标筛选、双端授权、排序及计数合并到正确的分页查询时，新入口明确不支持该组合；不取一页边再过滤对象后伪造 total，也不拉全量 ID。原关系管理的边分页继续使用原契约，不能冒充本接口的对象分页。具体支持范围见第 7.4 节的审查项。

请求 Bean 用于传输和构造，不充当不可变上下文；Manager 在方法调用时复制 ThingId、QueryParamEntity 及嵌套筛选/排序/投影，供后续订阅复用，不能只复制外层对象。执行身份、权限仍在每次订阅时读取；集群源端再次校验请求与来源归属。默认分页非空，显式传入 null 时按非法请求处理。

### 5.3 字段与结果契约

| 请求字段/结果 | 契约 |
| --- | --- |
| subject | 必填，固定导航主体；复制 type/id 形成请求快照，不长期持有可变 ThingId |
| relatedType / relationId | 必填且来自已声明关系，按 reverse 解析规范源/目标；不允许指定任意表或服务路由 |
| reverse=false | 查询 subject → related，返回 related 端记录 |
| reverse=true | 查询 related → subject，仍返回 related 端记录 |
| query | 默认空条件及原分页缺省值，仅筛选/排序/投影返回端字段；来源限制分页上限及实际支持的运算 |
| at | 可空的关系有效时点；不支持时失败，不与 query 中的发生时间条件混用 |
| PagerResult<ObjectData> | 复用原分页结构；每条 ObjectData 的 $metadata 声明其类型、角色及已返回关系，查询范围仍由请求保存 |

例：alarmOccurrence --target/about--> device；以 D1 为 subject、alarmOccurrence 为 relatedType、target 为 relationId、reverse=true，返回 D1 的告警发生，alarmTime 条件作用于告警而非设备。

实现先解析规范关系与返回端，再由权威来源查询，嵌套合并固定主体、用户条件与原权限。固定范围不得被 OR 移除，投影不能移除授权必需身份；不先拉全量 ID 逐条读取，也不能过滤掉一页未授权记录来冒充正确分页。计数与分页均须在正确授权范围内完成。

可读属性不自动等于可筛选字段。使用原 metadata(type) 发现集合字段，来源校验过滤及排序能力；例如 property.temperature 可读最新值，不意味着设备列表支持按此筛选。不支持的条件明确失败，不忽略 where 只应用分页。

旧 ObjectQueryParam/Provider 的方向和过滤注释有歧义，实施前以双向契约测试确定映射，不按 reverse 字面直接转发。旧 doQueryByRelatedPage 的 Mono.empty 表示未实现，新入口转换为不支持；支持但没有可见记录返回明确空 PagerResult。某方向不能受控分页时同样明确不支持。

现有 ObjectQueryParam 和旧接口保留；新请求由内部适配转换，不在 core 复制 QueryParamEntity，不维护第二套筛选语言。at 等新字段必须纳入集群能力协商；序列化后能读到字段不等于远端支持其业务语义。

### 5.4 ObjectData 的保留元信息：$metadata

每条 ObjectData 承载自身的类型、角色及关联语义，RelatedObjectQuery 保存本次查询范围，PagerResult 保存分页信息。三者职责分开，不另建重复承载请求上下文的结果包装。

采用保留字段 `$metadata`，继续使用现有 ObjectData 的 Map 结构；原业务字段不增加 values 包装层。元信息是显式声明，不靠字段名推断，类型化访问也不读取其他服务：

```json
{
  "id": "A1",
  "targetId": "D1",
  "alarmTime": 1793232000000,
  "$metadata": {
    "type": "alarmOccurrence",
    "semanticTypes": ["event", "result"],
    "relations": [
      {
        "relationId": "target",
        "semantic": "about",
        "related": { "type": "device", "id": "D1" },
        "reverse": false
      }
    ]
  }
}
```

这里是 A1 的数据，因此 relations[].reverse=false 表示 `A1 --target/about--> D1`。即使查询是“从 D1 反查告警”、请求 reverse=true，返回数据上的关系仍以当前行 A1 为起点解释；两处 reverse 的起点不同，不能照抄。

#### 元信息的类型化结构

在 relation-component 增加两个轻量可序列化值类型，省略访问器、构造及校验细节。它们当前只服务 ObjectData 行协议，不进入 core 的操作签名；出现独立的底层复用需求后再评估下沉：

```java
// org.jetlinks.pro.relation.metadata；数据实例的描述，不是完整 ThingMetadata 模型。
public class ObjectDataMetadata implements Serializable {
    private String type;
    private Set<String> semanticTypes;
    private List<ObjectLink> relations;
}

// org.jetlinks.pro.relation.metadata；相对当前数据行的已知关联。
public class ObjectLink implements Serializable {
    private String relationId;
    private String semantic;
    private ThingId related;
    private boolean reverse;
}
```

- type 在元信息存在时必填，作为对象/记录模型引用；semanticTypes 描述当前记录的角色，集合缺省为空。词汇复用 core 枚举，线缆保留字符串及未知新词；缺失角色不自动默认为 resource。
- 角色来自来源的类型/实例模型，不能由客户端或业务数据中的 status 等字段随意覆盖。统一模型的字段含义、单位、枚举仍由 ThingMetadata/PropertyMetadata 描述，不逐行嵌入完整模型。
- relations 缺省为空，只包含这次已返回的真实关联，不自动加载全部关系；空集合不表示该对象没有任何关系。related 为另一端真实身份，semantic 描述规范方向：false 为当前行→related，true 为 related→当前行。
- ObjectLink 描述已知关联，不等同于持久边实例。原关联记录、有效期及精确证据仍由来源和既有 RelationFact/EvidenceReference 表达，不因为出现 used/generated 标签就视为完整证明。
- 顶层 id 沿用 ObjectData 的来源稳定身份契约；type + id 才可组成对象引用。无稳定 ID 的样本不填 id、不拼伪 ID，可在当前结果内声明 about 主体，但不能作为持久血缘端点或调用 operations(id)。

不同数据各自携带含义；以下类型为场景示意，实际由来源模型声明：

| 数据 | $metadata.type / semanticTypes | 业务字段及关联 |
| --- | --- | --- |
| 设备 D1 | device / resource | 名称、状态及真实空间/分组联系 |
| 温度样本 | 来源观测类型 / observation | property/value/timestamp/quality；about D1，无原 ID 则不具备地址 |
| 单次告警 A1 | alarmOccurrence / event、result | alarmTime 等原字段；target/about D1 |
| 通知尝试 N1、N2 | 来源尝试类型 / activity | 各自状态/时间与原来源联系，不能仅凭 origin 标为 triggered |
| 指标结果 C1 | 来源指标类型 / observation、result | 值、窗口、原定义版本；有实际执行依据时声明 generated 的反向联系 |

同一类型的实例可以有模型允许的不同角色/已加载关联，但来源必须明确声明。单跳 RelatedObjectQuery 仍限定 relatedType；不同类型的数据由 ontology 组合多次查询，各行保留自己的 $metadata，不把这个接口扩大为任意混合查询引擎。

#### ObjectData 的最小优化

保留 `org.jetlinks.pro.relation.impl.ObjectData extends HashMap<String,Object>` 及旧 JSON 业务字段形状，不新增一套同名 ObjectData。类型化辅助入口如下：

```java
public static final String METADATA_KEY = "$metadata";

public Optional<ObjectDataMetadata> getMetadata();

public ObjectData withMetadata(ObjectDataMetadata metadata);

public Optional<ThingId> getReference();
```

getMetadata 从 Map 的保留键读取类型化视图；不存在返回 Optional.empty，格式错误明确失败，未知词保留。withMetadata 将已知字段写成规范 Map/List/字符串等可传输值，存入同一 `$metadata` 条目；不是只给 HashMap 子类新增一个不能保证进入序列化的 Java 成员。related 身份也编码为 type/id，线上不要求反序列化新增 Java DTO 或枚举类；不缓存一个可能与 Map 脱节的元信息副本。

类型化读取只解释已知字段，不将原 Map 读出后自动重写；未知扩展键透传保留，避免旧节点转发时抹掉新信息。ThingMetadata 描述可复用的模型，ObjectDataMetadata 只描述当前数据行的类型引用及已知关系，二者不互相替代。后者是 `$metadata` 的类型化值，不是另一套完整物模型。结果装饰显式接收来源对象类型与适用模型，`$metadata.type` 不使用可能为产品/模板 ID 的 ThingMetadata.getId()。

getReference 仅依据已声明 type 与来源稳定 id 返回身份快照，无元信息或无 ID 时为空，不通过 targetId 等业务字段猜身份；每次返回 ThingId 副本。实施时让身份读取与同一数据快照一致，并验证现有 getId 的缓存不会在 Map 修改后遗留旧身份。

元信息由来源/受信适配生成，不接受用户筛选、客户端字段或原始设备载荷声明执行身份/权限。新关联查询中每行 type 必须等于请求 relatedType，id 稳定且存在；至少返回与查询命中的关系相符的 ObjectLink，其 related 为请求 subject、reverse 为请求 reverse 的反值，semantic 与规范定义一致。额外联系只按来源明确提供的内容返回，不触发隐式扩展查询。

统一适配层负责补 Provider 类型、已有角色及本次命中关系，原业务埋点不用逐条手写元字段。来源只贡献真实的额外语义/联系；不为填 `$metadata` 逐行调用 getMetadata、加载其他关系或访问远端。需要原产品/模板才能确定的字段模型继续按需解析。

#### 保留字段与兼容边界

1. `$metadata` 仅在统一对象数据协议中为保留键，不新增数据库列、索引、常驻采集或注解扫描。原业务数据及既有入口不强制改写；旧来源无此键仍可读取，语义保持未知。
2. 新请求适配旧来源时，按 Provider、来源稳定 ID 和真实关系结果补齐元信息；不修改共享原记录。来源原字段已叫 `$metadata` 时明确报冲突，或由该来源显式投影别名保留原值，不能覆盖或丢弃业务数据。
3. 用户投影不能删除协议元信息和身份；where/order 仍使用来源已支持的业务字段，不能把 `$metadata.*` 当作自动可检索字段。所有实际读取与命令仍在来源授权，元信息本身不授予权限。
4. 对外响应可按新接口/能力版本启用元字段，旧接口默认保持原形状；不能假设旧客户端一定接受新增键。集群验证纯 Map 元字段的往返、类型化辅助读取及身份编码，不序列化操作句柄或直接传新增 DTO 类。角色/关联组装只用已获得的模型和关系，不能逐行发额外 I/O。

#### 直接复用分页结果

Manager 和 Provider 的新查询重载均返回 `Mono<PagerResult<ObjectData>>`，普通分页与关联分页使用同一结果形状。主体、方向、关系和 at 保存在请求；实际返回数据的类型和联系保存在每行 `$metadata`。空页返回原空分页，不为回显查询条件增加结果类型。

来源成功返回时必须满足完整请求语义；不支持 at 就失败，不能因响应不回显时点而退回当前关系。at 不等于行内事件时间或边有效期，不复制成数据时间戳。需要离线导出、历史审计或解释空结果时，由调用方/ontology 保存请求快照和实际结果；孤立的分页不能被声称包含完整查询证据。

分页单位仍是另一端可见对象；total 不是关系实例数。N1/N2 等不同尝试保留原 ID，不按来源去重；同端点多条边的不同时间/证据仍查询原关联记录或既有边事实。空分页只表示本次没有可见记录，不证明不存在关系或业务恢复。

属性标量和普通命令输出不强制包装成 ObjectData：含义继续由 PropertyMetadata、FunctionMetadata/DataType 描述。需要作为统一对象/记录参与跨业务组合时再适配 `$metadata`；模型历史版本缺失时保持未知，不用当前模型冒充当时模型。

### 5.5 各类数据的读取路径

| 所需数据 | 实际读取入口 |
| --- | --- |
| 当前值/状态 | properties().get(原读取路径) |
| 空间、分组、负责人 | relations(reverse) 按声明导航 |
| 有身份的观测、事件、执行、结果 | 按模型识别语义及字段，再用 queryRelatedPage 读取原记录 |
| 无稳定 ID 的遥测 | 原 ThingProperty/QueryOperations，或有明确输入输出的只读功能 |
| 按需计算 | commands() 中的 query |
| 确认、控制、重试 | commands() 中的 action，执行原授权和状态校验 |

不增加 ObjectDataOperations 或 queryId + Map 的万能数据目录。无 ID 遥测保留 property/value/timestamp/state，不拼时间戳伪 ID、不调用 operations(id)，不成为精确血缘端点；确需追踪时由权威来源保存真实输入引用。

## 6. 声明与消费示例

### 6.1 原字段不变，补语义

以当前 AlarmOccurrenceSource 已有字段为基础，下面展示模型声明方式。告警扩展已补齐 event/result、identity 和 occurredAt；target 的 about 沿用原声明，原业务不依赖 ontology：

```json
{
  "id": "alarmOccurrence",
  "expands": { "semanticTypes": ["event", "result"] },
  "properties": [
    { "id": "id", "valueType": { "type": "string" },
      "expands": { "semanticRole": "identity" } },
    { "id": "targetId", "valueType": { "type": "string" } },
    { "id": "alarmConfigId", "valueType": { "type": "string" } },
    { "id": "alarmTime", "valueType": { "type": "date" },
      "expands": { "semanticRole": "occurredAt" } }
  ],
  "relations": [
    { "targetType": "device", "id": "target", "reverse": false,
      "expands": { "semantic": "about" } }
  ]
}
```

ThingMetadata.toJson 支持可选 relations/modules，每个目录单次读取。组合模型按目标类型/关系 ID/方向索引合并，比较描述值，不做 JSON 比较；原模型可变，不永久缓存。空目录不改变已发布旧模型形状；本组 PR 中尚未发布的 Map 目录改为含 targetType 的列表。jetlinks-supports 的默认设备/通用 Codec 统一保留根身份、关系、嵌套模块和扩展；relation 的 RPC 直接使用默认 Codec，不再维护重复解析器。SimpleDeviceMetadata 原有的部分 fromJson 不作为完整 Codec。代码声明复用 expand：

```java
alarmTime.expand(MetadataSemantics.PROPERTY_ROLE,
                 PropertySemanticRole.occurredAt.name());
```

只有跨业务发现、导航或调用时才接入；模块内部中间变量无需建模。仅补语义时扩展元数据，已有 Provider 需操作能力才覆盖 operations，新记录类型才增加来源 Provider。声明事件不会自动获得历史存储。

新来源的关键结构如下，省略身份、关系等常规实现。示例类及内部方法表示原业务适配，不是额外通用 SPI 或新命令 ID：

```java
// Provider 中
@Override
public ObjectOperations operations(String id) {
    return new AlarmOccurrenceOperations(id); // 仅捕获身份及原业务依赖。
}

@Override
public PropertyOperation properties(String id) {
    return operations(id).properties(); // 单向兼容，不能互相默认回调。
}

// 自有 AlarmOccurrenceOperations 中
public Mono<? extends ThingMetadata> getMetadata() {
    return Mono.defer(() -> modelFor(id));
}

public PropertyOperation properties() {
    return key -> Mono.defer(() -> readOccurrenceProperty(id, key));
}

public CommandSupport commands() {
    return commandHandle; // 原绑定实例的适配；无命令时是不支持句柄。
}
```

命令适配绑定当前身份，输入 id/deviceId 等保留字段不能覆盖主体；只公开适用于该对象的原处理器，不暴露整个业务服务。若原“确认告警”处理的是告警单/配置，应先导航到该对象，不能给单次发生记录挂上别的主体的动作。

### 6.2 调用方式

```java
// 无需实例即可发现类型模型。
relationObjectManager.getMetadata("alarmOccurrence");

// 沿用实际读取路径，不能把 temperature 误当已有顶层属性。
relationManager.getObject("device", deviceId)
    .flatMap(device -> device.properties().get("property.temperature"));

// criteria 使用告警原字段，例如 alarmTime。
RelatedObjectQuery request = new RelatedObjectQuery();
request.setSubject(ThingId.of("device", deviceId));
request.setRelatedType("alarmOccurrence");
request.setRelationId("target");
request.setReverse(true);
request.setQuery(criteria);
relationObjectManager.queryRelatedPage(request);

// 动态工具调用：功能 ID 与参数来自实际元数据。
relationManager.getObject(typeId, objectId)
    .flatMapMany(object -> object.commands().executeToFlux(functionId, arguments));
```

已知命令类型的业务继续优先使用原显式 Command。动态页面/大模型才需要按元数据发现功能；query/action 不代替调用授权和动作确认策略。

### 6.3 复杂 IoT 场景落点

```text
温度观测 P1 ─about─────> 设备 D1 ─structural─> 空间 S1
场景执行 R1 ─used──────> P1、其他实际输入
场景执行 R1 ─generated─> 告警发生 A1 ─about──> D1、其他关联设备
告警发生 A1 ─triggered─> 通知尝试 N1、N2
后续处置执行 ─used─────> A1，并关联原任务及核验结果
```

图中为语义标签，不要求与原关系 ID 同名。只有来源真实保存 P1/R1/触发引用才展示相应因果边；当前告警 target、通知 origin 首先证明归属/来源，不能单独证明精确计算输入。

N1 失败、N2 重试成功保留独立尝试 ID，不按告警去重，不凭相邻时间推断 N1 导致 N2。聚合告警、多来源通知保留多对多关系，不固化单父设备树。处置成功后读取原核验结果/恢复观测，由 ontology 判断效果，命令成功不等于闭环完成。

历史空间/分组关系需要来源有效期或发生时快照。观测/发生时间、保存时间、关系有效时间和查询时间分别保留；缺少依据保持未知，空可见结果不证明没有关系或业务恢复。

## 7. 本体场景覆盖与架构边界

### 统一语义声明与消费（已确认）

通用语义由 core 的 `MetadataSemantics`、字段角色和注解维护；relation 负责带逻辑来源的能力描述与传输，本体只消费并投影。沿用 ThingMetadata、DataType 和原 Command，不新建对象模型、查询框架或授权入口。

实施范围：统一内置字段角色及扩展键，为观测值提供类型化的时间/质量字段映射；将有界 schema 路径及记录读取收敛到 core 的只读语义视图；relation 集中编码来源描述；迁移本体消费与设备快照注解。类型、单位、枚举由原值模型提供，窗口由已有开始/结束角色表达，缺失声明保持未知。旧 `value` 角色与 `presentation.observedAt` 在 core 读取边界解释，现有传输字段及开放词汇保留，业务 Map 数据不强制改成固定 DTO。

步骤为 core 契约、relation 描述、本体和已有声明迁移、集中集成验证。只校验已声明的映射形状和字段引用，不新增逐次权限/实例预检、常驻状态、后台任务或 Trace/MBean；实际读写仍使用原上下文和来源授权。验证覆盖注解与序列化、旧格式、未知扩展、0/false、多个结果、时间/质量绑定、同名来源、图谱投影及原查询/操作回归。无服务启停或业务功能扩展。

实现入口为 `metadata/MetadataSemantics`、`PropertySemanticRole`、`ObservationMapping` 与 `SemanticRecord`。`semanticTypeTerms` 返回包含未知词的有序只读集合；`propertyRole` 将旧 `value` 解释为 `result`，不修改原 expands。`subjectId`、`modelId`、`valueMetadata`、`formattedValue` 补齐原字段含义；记录身份与被描述对象身份仍分开。

结果值可使用 `@SemanticRole(result)`、`@Observation(observedAt = "sampledAt", quality = "valid")`，时间、质量字段继续使用原 `@Schema`、`@SemanticRole` 和 DataType。观测映射沿用已有 `presentation` 线上键，不新增平行编码，旧 `@Presentation(observedAt=...)` 同样适用。质量字段类型由来源决定，读取保留 `false`/`0`，不把它转换成统一业务状态。

需要跨来源识别领域概念时，可在原字段上附加 `@SemanticId("urn:factory:property:temperature")`；动态模型使用 `MetadataSemantics.withSemanticId(field, conceptId)`，读取使用 `semanticId(metadata)`。例如传感器的 `value` 与巡检记录的 `ambientTemperature` 可指向同一温度概念，各自字段 ID、result 角色、读取路径、单位和采样时间仍保留；共享概念不等于两个值可直接比较。概念 ID 由来源维护，不要求业务连接术语服务。原 `RelationMetadata` 可用 `withRelationSemantic(relation, RelationSemantic.about)` 或来源自定义词声明分类，`relationSemanticTerm` 保留完整词，枚举读取仅返回已认识的分类。

`SemanticRecord.field/value/fields` 只读声明路径，深度上限 8，不跨数组取值或聚合；`roleValue` 遇到多个同角色字段返回未知，调用者应显式选路径。`observedAt`、`quality` 相对于当前字段所在记录读取；新声明缺少时间映射时保持未知。仅旧 `value` 别名兼容“唯一结果＋唯一采样时间角色”，不把查询时间或其他结果时间作为兜底。单位、枚举、精度继续来自原属性模型，不增加万能值 DTO。

契约回归入口为 `SemanticRecordTest,MetadataSemanticsTest,PresentationAnnotationsTest,ExpandsMergesTest,ExpandsMergeTest,MetadataUtilsTest,CommandMetadataResolverTest,MergeOptionTest`；relation 与消费者验证入口见配套本体 `docs/development-plan.md` 的同名条目。运行服务与混合版本部署仍需配套验收。

#### 用业务记录理解这些声明

整体分类、字段用途、功能用途和对象关系是不同维度。以当前设备属性快照为例：

| 声明位置 | 对应内容 | 消费者应如何理解 |
| --- | --- | --- |
| Snapshot 模型 | observation + result | 这是一条观测结果，不代表创建了另一个设备对象 |
| deviceId | GraphSubject，与 subjectId 表达相同用途 | 这条快照描述哪台设备；快照自身若有 ID，应另用 identity |
| productId | modelId | 采用哪个产品的属性模型，不改变对象类型 device |
| metadata | valueMetadata | 原属性定义，例如 temperature 的数值类型、摄氏度单位和精度 |
| value | 现有 value 别名，读取为 result | 参与计算的原始值，例如 21；不是“21 ℃”显示文本 |
| formattedValue | formattedValue | 来源格式化后的显示文本；不能据此求平均值 |
| timestamp | observedAt，且 value 显式映射到此字段 | 原采样时刻的 Unix 毫秒值，不是本次查询时刻 |

告警记录的 alarmTime 表示事情何时发生，适合 occurredAt；温度补报数据的入库时间适合 recordedAt，二者都不能替代原采样时间。质量描述值是否可信，例如来源额外提供“估算值”或 valid=false，不等于设备在线状态或告警处理状态；当前设备快照没有额外质量字段，不会凭空补出质量。通知的一次发送尝试是 activity，重试仍是独立尝试；发送请求受理不等于发送成功。

时间、质量路径相对结果字段所在的记录：sensor.value 映射 timestamp，读取 sensor.timestamp。多个观测优先用各自子记录或原多行结果保留值、属性定义、时间和质量的对应关系；整体分类和字段角色本身不会自动完成查询、图谱关联或因果证明。

#### 三轮对照审查与收敛

第一轮对照语义。参考 [SOSA/SSN 的观测与属性](https://www.w3.org/TR/vocab-ssn/#SOSAobservedProperty)、[现象时间](https://www.w3.org/TR/vocab-ssn/#SOSAphenomenonTime)与[结果完成时间](https://www.w3.org/TR/vocab-ssn/#SOSAresultTime)，结果、所描述属性、被描述对象、适用时间分别表达。`SemanticRecord.resultQualifier/resultPropertyId` 仅关联当前层唯一结果和唯一说明；多结果不共享未绑定的定义、单位或显示值，优先使用各自子记录或多行输出。`observedProperty` 参与结果关联，不再单独输出为无关数值。两个身份声明矛盾时保持未知，payload 不丢弃。未知角色通过 `propertyRoleTerm` 区分“有声明但不认识”与“没有声明”，结构化值不被隐式拆分。时间采用精确、有符号 Unix 毫秒，拒绝小数、非有限数和溢出；纪元前时间合法。

平台的 observation 是观测数据分类，投影中的 Observation 是观测值节点；SOSA 的 Observation 则指执行观测的活动，当前没有断言二者等同。[PROV-O](https://www.w3.org/TR/prov-o/#description-starting-point-terms) 的 Entity、Activity、Agent 及 used/generated 提供职责参考，providerId 仅表示逻辑读取来源，不能自行证明因果或责任。[RDF 1.1 的空白节点](https://www.w3.org/TR/rdf11-concepts/#section-blank-nodes)只在本次响应内标识事实，不能作为持久记录身份。输出使用平台词汇，未宣称完整实现 SOSA、PROV 或推理引擎。

第二轮对照业务接入。静态来源复用原 `AbstractObjectSupport`、`@ObjectSupport`、`@CommandHandler` 和 `@PropertySource`，没有增加反射调用器或新模板。单值摘要、观测、事件和动作均用同一路径发现/执行；`@PropertySource` 已含 query 用途，无需重复注解。实例动态模型与批量属性可覆盖 relation 原模板的 `getMetadata(objectId)`、`properties(objectId, commands)`；复杂来源仍实现原小接口，完整约定见配套 components README。动态模型可使用以下写法，不必手拼扩展键或嵌套 Map：

```java
var output = withSemanticTypes(new ObjectType(), SemanticType.observation, SemanticType.result);
var value = withPropertyRole(
    SimplePropertyMetadata.of("count", "当前告警数", LongType.GLOBAL),
    PropertySemanticRole.result);
withObservation(value, new ObservationMapping("sampledAt", "valid"));
output.setProperties(List.of(value,
    SimplePropertyMetadata.of("sampledAt", "采样时间", LongType.GLOBAL),
    SimplePropertyMetadata.of("valid", "数据有效", BooleanType.GLOBAL)));
// 完整输出装配后核对声明路径，不逐次预检业务实例或权限。
MetadataSemantics.validateOutput(output);
```

以上静态方法来自 `MetadataSemantics`，返回原可修改模型，共用原默认合并规则，保留 primary 和未知展示项。`sampledAt`、`valid` 是来源 schema 中已声明的字段路径，业务 ID 和自定义角色仍由业务命名，不把它们搬进通用枚举。`name/severity/description` 等原展示角色不扩成新的核心分类。来源方法应委托已有授权服务/命令，目标并非自动代理的 Spring Bean，不能依赖目标方法 AOP 自动生效。

第三轮对照结构与跨来源契约。core 统一声明和记录读取，relation 统一来源发现与传输，本体只匹配已授权引用并输出图；不恢复 Provider 实例预检，不猜第一条/最新条，不因失败切换来源。属性定义保持原 `PropertyMetadata` JSON，在投影边界原样携带远端自定义类型和扩展，避免消费端重新解码后丢字段或要求注册同一业务类型。任意业务 payload 依据 [JSON-LD 1.1 JSON literal](https://www.w3.org/TR/json-ld11/#json-literals)保留，不从其任意键推断图关系；这也意味着 payload 内部不会自动成为可查询的 RDF 事实。

验证入口为 core `SemanticRecordTest/MetadataSemanticsTest` 与配套本体 `ContextGraphProjectionTest/ObjectSemanticsReuseTest`。后者使用真实注解、命令适配、模型解析和图谱投影，业务 I/O 以测试来源代替；已有 relation 回归覆盖同名来源、RPC、上下文、空值与故障。本轮 335 项全部通过，四个原缺陷探针已输出纠正结果，13 个公共语义类/注解 Javadoc doclint 无诊断，四仓库 diff 检查通过。此前 328 项仅为上一阶段证据。没有增加缓存、后台资源或业务执行链路，追踪与 MBean 无需新增；混合版本和运行服务待部署验收。

结论：这套 SPI 可以作为 IoT 业务本体的来源访问层，覆盖对象、属性、关系、数据记录及动作的发现和调用；它不是完整本体运行时。不能仅凭接口和语义枚举，就认定任意历史、因果、推理或业务闭环都已实现。

下面评价的是设计可承载程度，不是当前 Java 实现交付状态：

| 真实场景 | 覆盖程度与前提 | 所属层 |
| --- | --- | --- |
| 设备、空间、分组、自定义维度统一查看 | 可承载；来源提供真实属性及关系，动态关联复用原关系服务 | ObjectOperations + relation |
| 按空间查设备，再查设备告警/通知 | 可组合；每一步做有界查询、授权及分页，不保证任意跨源 join 能下推 | relation 单跳；ontology 组合路径 |
| 设备遥测、指标结果、AI 告警统一理解 | 可描述并适配原读取；单位、时间、质量及结果类型保留，时序/聚合继续用原数据能力 | 来源模型/仓库；ontology 组合 |
| 某次告警的多设备目标、通知失败/重试 | 可承载；每次发生/尝试有原身份，不按目标或来源去重 | 原发生/执行记录及语义关系 |
| 一次计算用了哪些样本、产生哪些结果 | 有条件；原记录或 Monitor 明确保存输入、输出和活动引用后，才能声明 used/generated | 原业务采集；ontology 证据解释 |
| 告警发生时设备属于哪个空间 | 有条件；at 能传递查询意图，但来源必须有真实关系历史/快照；当前关系不作历史证据 | 历史来源；ontology 有效时间 |
| 一条关系的证据、有效期及多次建立/解除 | 结果声明命中关系的语义及方向；边实例差异及证明仍沿用原关联记录或 ontology 的 RelationFact/证据适配 | 关系所属来源 + ontology |
| 动态创建业务模板、切换主体模型 | 组合层可承载；模板选择已有来源及关系，不改变底层对象身份；动态声明不自动得到实例存储 | ontology 管理/发布 |
| 运行时增加全新对象类型及实例 | 有接入边界；除模型外必须提供权威数据来源和管理能力，ObjectOperations 不自带通用对象数据库 | 动态来源/原业务；原关系服务 |
| 确认告警、控制设备、联动外部系统 | 可承载实际公开的原命令；跨系统事务、幂等及补偿不由对象门面保证 | 原命令/场景/工作流 |
| 实时事件、持续更新和订阅 | 可发现定义；执行订阅仍复用原管理器，尚无 core 统一事件句柄 | 原事件运行时 |
| 按模板完成闭环、输出是否恢复 | 模板表达流程及完成条件，需实际核验依据；命令成功不自动等于效果成功 | ontology + 原业务核验 |
| OWL 类推理、规则蕴含、SHACL 约束、任意图查询 | 当前不覆盖，也不是本次基础 SPI 的目标；确有需求再接专用能力 | 可选的本体引擎/校验工具 |

### 7.1 仍需遵守的结构契约

1. **类型、实例和发生记录分开。** 模型版本不等于对象 ID；动作定义不等于活动执行；事件定义不等于已保存事件。语义角色只是声明，不自动创建记录。
2. **数据含义与查询上下文分开。** ObjectData.$metadata 声明每条数据的类型、角色及相对关系；请求表示查询范围，PagerResult 表示分页。关系实例的不同时间、来源和证据仍查询原关联记录或既有边事实，不把元字段当作精确来源证明。
3. **二元关系可组合多参与方。** 一次活动关联多个设备、输入、通知接收人及输出；需要描述组合关系时以原活动/关联记录为中心建模，不新增大量业务专用 SPI，也不固化成树。
4. **发现能力必须可信。** 角色为 event 不代表能订阅，声明 about 不代表支持反向分页。集合字段沿用原 metadata(type)；本体适配仍使用已有 sourceFeatures，通用来源自身的支持范围须在 relation 层声明并纳入集群协商，不反向依赖 ontology。未声明能力保持未知，不能通过枚举、方法存在或吞错推断支持。
5. **组合需要有界执行。** 深度、节点量、并发及时间预算放在 ontology 查询/上下文扩展层；单跳请求不增加递归路径 DSL。重复主体批量读取优先复用原批量接口，实际存在性能问题再扩展批量请求。

### 7.2 需要收敛和明确归属的内容

| 内容 | 审查结论与落点 |
| --- | --- |
| 模型及语义 | 统一到 ThingMetadata 及既有字段/功能/关系模型的 expands；core 只增加 ObjectOperations 和通用语义词汇/工具，不另建并行模型、万能数据操作目录或功能注册中心 |
| 行数据协议 | ObjectDataMetadata/ObjectLink 留在 relation-component，复用原 Map 和 PagerResult；它们不是完整模型、边事实或结果证据，不为查询回显再包一层结果 |
| 关系实例、时间和证据 | 原关联记录继续由来源管理，RelationFact/EvidenceReference 留在 ontology。来源接入不能因去掉业务对 ontology 的依赖而删掉原证据、历史能力 |
| 模型迁移及兼容 | 语义扩展、可选关系目录和独立模型获取分步验收；保留单向适配和原 Provider 路由，不为消除一个接口连带改造全部物模型持久化格式 |
| 业务执行及推理 | 时序、指标、规则、通知、任务、订阅复用原能力；对象门面不承诺跨源事务、自动幂等、通用图查询或本体推理，不新增空的扩展接口占位 |

### 7.3 从本体概念复核

结论：模型、实例、属性、关系、观测、活动及操作具有对应位置，适合作为轻量 IoT 业务本体的访问基础。只新增 ObjectOperations，模型与语义直接增强通用物模型；仍复用 `PagerResult<ObjectData>`。下面按 2026-10-29 读取的官方规范/模型说明复核，参考固定版本的含义，不声称实现其标准格式或满足全部规范。

| 文献及关键依据 | 对设计的要求 | 当前取舍 |
| --- | --- | --- |
| [WoT TD 1.1 §5、§7.1](https://www.w3.org/TR/wot-thing-description11/)：交互、数据结构和语义注解分开；动作可描述 safe/idempotent/synchronous | 模型应可复用，语义不承担执行绑定；query/action 不能替代安全重试和结果完成语义 | 保留原属性/功能/事件及 Command；来源声明真实执行契约，不增加统一动作执行器 |
| [SOSA/SSN 2017 §4.3、§4.7](https://www.w3.org/TR/2017/REC-vocab-ssn-20171019/)：Observation 是观测行为，区分 Sensor、FeatureOfInterest、Result、phenomenonTime 和 resultTime | 上报设备、被测对象、数值、适用时间和结果完成时间分别表达；时间可为区间 | 本平台 observation 是记录角色，不自动等同于 SOSA Observation；保留原窗口字段和业务关系，不把 recordedAt 当作 resultTime |
| [SAREF Core 4.1.1：Properties 与 Property Values](https://saref.etsi.org/core/v4.1.1/)：通用属性、某主体的属性、带单位的属性值分开 | temperature 这个领域概念、字段 result 角色和当前读取路径是不同维度；同名不同单位不能直接比较 | 原 DataType/单位/质量不变；外部概念引用按需扩展，不引入自动单位换算或概念匹配引擎 |
| [PROV-O §3.1、§3.3](https://www.w3.org/TR/prov-o/)：Entity/Activity/Agent、派生及 qualified relations | used/generated 依赖真实执行依据；不知道活动时也能保留实体间已知派生；多参与方以原记录承载限定信息 | Actor 沿用用户/系统对象；直接派生见 7.6，不虚构活动、不把 about/trace 当作数据依赖；当前词汇不宣称与标准谓词等价 |
| [OWL 2 Primer §2–3](https://www.w3.org/TR/owl2-primer/)：类型/实例与开放世界假设 | 没查到数据不证明事实为假；业务视图不是自动产生的新本体类 | 类型/实例/模型 ID 分开；依据不足仍为 unknown。未实现类继承、等价类、蕴含和自动分类，Java 继承也不代表本体蕴含 |
| [SHACL §3.6](https://www.w3.org/TR/shacl/)：校验结果定位到焦点对象、路径和约束 | 动态声明需要可定位的发布校验，来源约束与运行证据不能混为一类 | 复用原模型校验、DataType 和业务状态机；至少定位模型元素/字段路径及原因，不把动态声明当任意执行配置，不引入 SHACL 引擎 |
| [OpenLineage Object Model](https://openlineage.io/docs/spec/object-model/)：Job、每次 Run、输入输出和可扩展 facets | 定义/执行/尝试分开，版本和实际输入输出保存于来源；扩展不能破坏原生命周期 | 复用原 Monitor/业务记录；不增加第二套运行表，技术 COMPLETE 不等于业务效果已经核验 |

五个 RelationSemantic 是常用归类，不是关系全集。安装位置、上报者、被测对象、负责人、来源系统可以使用不同业务关系 ID；没有合适通用分类时保留原关系和未知语义。来源仅保存“结果依赖另一结果”时，不为套用 used/generated 虚构活动，也不以 about 代替数据依赖。只有出现多个真实来源和消费方都需要统一理解的新词时，才扩展通用词汇。

对象身份在可信项目/租户和权威来源范围内解释。不同外部系统恰好都有设备 D1，不因此成为同一个对象；接入方须在既有类型/ID 映射中区分来源。跨范围解析必须重新授权，不因 `$metadata` 或传入引用而改变租户或来源路由。首期不增加全局身份注册中心。

### 7.4 首期必须补齐的最小契约

本节维护六类契约及验收要求，首期实现范围见第 9 节。能力仅向确实实现的来源开放，不要求所有旧模块同时升级；来源未支持的历史、筛选或对象分页明确失败。

| 审查项与落点 | 代码事实与实际风险 | 最小补齐与验收 |
| --- | --- | --- |
| 读取路径；relation/来源 | DeviceObjectProvider 读取 `property.temperature`，原字段可叫 temperature；集合 metadata() 不是全部实例属性 | 来源通过窄扩展或模型投影声明读取路径，调用方不猜前缀；测试字段到读取路径的对应关系及不可筛选属性的明确拒绝 |
| 权威关系、授权与分页；relation/来源 | DefaultRelationOperation 区分固定/管理关系，当前 Manager 分页只调 Provider；边分页不等于对象分页 | 按实例归属路由，双端授权、OR、投影、排序和计数使用同一范围；跨源不能受控分页就明确拒绝，不能页后过滤伪造 total |
| 真实能力发现；relation/集群 | sourceFeatures 仍在 ontology；语义枚举或空目录不能表明每个关系/方向的实际支持范围 | 按关系/方向投影读取、分页、历史支持到已有 expands；命令发现复用真实 FunctionMetadata。缺省未知、执行再次校验；旧节点和不支持能力不得静默降级 |
| 模型、版本、时间与含义；core/来源 | type 不等于产品模型/版本；一个时间点不能表达计算窗口、结果完成和关系有效期 | 原产品/版本、单位、质量和窗口字段不丢失；只按已知口径读取，不任取首行模型或跨单位比较。身份及查询 at 保持原契约；旧版本/历史缺失明确未知 |
| 操作及完成语义；来源/原 Command | query/action 与 Mono/Flux 不说明可否重试；默认 executeToFlux(Command) 会直接调用 execute，异步任务响应也可能只代表受理 | 按订阅绑定主体/上下文；默认不自动重试。确有来源契约时描述幂等及任务回执，绑定参数不能覆盖主体。验收远端超时、重复订阅、取消和受理/执行/核验的区别，不新增万能动作结果 DTO |
| 编码、合并与声明校验；core/relation/ontology | ThingMetadata.toJson 缺关系；SimpleObjectType 固定编码，组合视图未合并新增目录；当前默认值也不能修复旧源分页授权 | 验证旧实现兼容、关系/未知词往返、循环有限输出及合并冲突；校验失败定位对象/字段路径/原因。错误、不支持和空结果保持区分，旧客户端字段形状按版本保留 |

这些是“来源可以正确接入”的缺口，不是增加更多本体名词的理由。尤其是分页权限，给旧结果补 `$metadata` 无法修复错误计数；先修权威查询或明确限制能力，再公开新入口。

### 7.5 用复杂业务区分覆盖与缺口

以“冷库多探头 + 压缩机 + AI 检测 + 聚合告警 + 通知重试 + 巡检核验”为验收场景。以下是待实施契约和来源接入要求，不是新增测试通过结论。

| 业务步骤 | 此设计可承载的内容 | 不能靠声明自动获得的内容 |
| --- | --- | --- |
| 探头 D1/D2 上报，设备属于冷库 S1 | 原属性/历史读取；分别表达采集设备、被测冷库及安装关系；样本保留真实单位、质量和时间 | 无原样本 ID/版本时，不能凭设备 + 时间伪造精确输入引用 |
| 窗口指标与 AI 结果共同触发 R1/A1 | 原结果保留各自模型/版本；来源保存实际选定输入后，R1 通过 used/generated 连接多输入与告警 | 设备都在同一空间、查询都返回了数据，不证明所有数据都被使用；动态模板不替代窗口计算和规则执行 |
| A1 联动压缩机 C1、通知 N1/N2、巡检 T1 | 原命令执行；人工 A1/C1 联系或设备联动关系由原关系服务维护；通知不同尝试、处置任务分别引用 | 命令返回、任务受理和通知通道成功不证明物理执行、用户收到或故障解除；幂等/重试沿用原业务 |
| 设备随后迁入 S2，回看 A1 发生时上下文 | 历史来源支持时通过 at 查发生时关系；当前处置仍按当前对象与权限执行 | 只保存当前绑定时不能恢复 S1；来源无历史必须拒绝，不能展示 S2 冒充当时状态 |
| 判断温度恢复并完成闭环 | ontology 组合原巡检结果、连续恢复窗口和质量条件，分别返回满足/不满足/依据不足 | 空样本、权限裁剪、采集关闭、记录过期和单次命令成功都不能自动判定闭环完成 |

因此可实现统一查看、关联取数、执行原操作及有依据的闭环解释；完整数据血缘仍依赖真实输入输出、执行/尝试身份、版本和证据的持久化。Monitor 提供低侵入记录与上下文传播，不能从调用链自动还原业务因果。实时订阅、工作流及形式推理由原能力或以后确有需求的专用扩展承担，不继续加入 ObjectOperations。

### 7.6 预留扩展方式与启动条件

预留的是可兼容的语义和接入边界，不是提前编写空 SPI。默认继续使用原模型 expands、独立请求 DTO、原命令参数/输出和证据引用；新增选项必须明确类型、缺省、支持范围及不支持行为，不能靠任意 options 参数袋接住一切。

| 扩展方向 | 启动条件 | 最小接入方式与限制 |
| --- | --- | --- |
| 外部语义概念 | 两个来源确实需要统一识别同一领域属性/动作 | 可选 expands.semanticId 引用稳定 IRI/领域概念 ID，与 semanticRole 及读取路径分开；先约定键及校验，不实现自动解引用/术语服务，不按同名或同 IRI 自动判定业务等价 |
| 结果直接派生及参与者 | 来源保存了结果到输入的真实依赖，或确需解释执行责任；现有 used/generated 不能表达 | 原业务可先声明独立关系；多个来源/消费方共同使用时再纳入 derivedFrom（结果→原输入）等通用词。参与者复用用户/系统对象，不把当前查看者当原执行者，不虚构缺失活动 |
| 模型/数据版本与历史 | 来源实际保留旧版本、冻结快照或绑定有效期 | 沿原记录/版本服务引用；多个来源需要相同解析能力时再统一引用结构。at 仍只表示关系有效时点，不预加所有双时态字段或统一快照仓库 |
| 关系实例及限定信息 | 多个来源都需要区分同端点多条边的证据、角色、有效期或置信度 | 先复用原关联记录与 RelationFact；有共性再评审 relation 层边实例查询。对象分页不改为边分页，AI 置信度不自动成为确定关系 |
| 动作幂等和异步办理 | 来源已有幂等键/回执/任务查询，多个通用客户端确实需要消费 | 在 FunctionMetadata.expands 或原输入输出模型中按来源声明；safe、idempotent、受理和完成分别解释，未知保持未知。声明不代替原幂等实现，也不触发门面自动重试 |
| 批量、游标及跨源性能 | 实测有重复远程读取或大集合分页瓶颈 | 优先原批量 Command/索引，必要时给独立请求增加有缺省行为的字段；不逐行 I/O、不扩成跨库 join 或任意图查询引擎 |
| 统一实时订阅 | 至少两个真实消费者需要同样的对象级订阅与取消/恢复语义 | 先复用现有事件管理器；只有原生命周期契约无法覆盖时再评审对象事件入口，不因模型声明了 event 就新增常驻订阅 |
| 标准导出、约束或大模型查询入口 | 有明确互操作方、验证规则或受控工具需求 | 专用适配器把模型映射为 WoT/JSON-LD/SHACL 等；大模型或 GraphQL 仅消费已公开的有界能力。SPI 不强制 RDF，不开放任意服务/表达式，不另写推理或流程引擎 |

### 7.7 最小语义契约与事实复用（已实施）

目标是补齐已经需要的声明入口和消除重复事实结构，不以覆盖规范为由扩充对象框架。范围为 core 的语义声明、components 的事件基础设施及 ontology 的原事实消费，不新增业务接入。

1. core 为原 `Metadata.expands` 增加可选 `semanticId` 的统一键、类型化读写和轻量注解，表示稳定的领域概念引用。例如两个不同字段可引用同一个温度概念，但字段 ID、角色、读取路径、单位及来源仍分别保留。沿用原合并入口处理矛盾声明；只检查声明的基本字符串形状，不验证远端 IRI、不请求术语服务、不推断业务等价。
2. 补齐原关系分类的开放词汇读写，仍使用 `RelationMetadata`、`RelationSemantic` 和既有 `semantic` 编码。规范 source/relation/target 与反向浏览分开；`structural` 不蕴含传递性、对称性或权限继承。没有真实共用需求，本轮不增加基数、类继承或关系约束执行器。
3. ontology 的路径结果直接复用原 `RelationFact`，删除 `ResolvedPath.Link` 的字段复制和转换。保留 record、evidence、半开有效期及关系实例属性；JSON 字段形状不变，Java 调用方配套迁移。`EvidenceReference` 的查询范围和能力参数留在 ontology，不下沉为 core/relation 的通用执行协议；`ObjectLink` 不冒充完整关系事实。
4. 配套 components 复用原 Topic 工具、跳过无关事件的异步登记并同步主题追加/快照；具体边界见其 `README.md` 的“语义声明与事件处理边界”。Topic 接收使用 `CharSequence`，优先 `SharedPathString` 模板与分段重载，原路由形状保持。分段序列的 equals 区分实现类，事件在去重边界统一为 SharedPathString，不在各层编码成字符串。

兼容边界：新增可选声明对旧模型缺省无影响。路径 Java 类型、公共资产发布器的 topic 返回类型及未合并的 scope 配置键需要配套重新编译/升级；只替换依赖而保留旧设备字节码会产生 AbstractMethodError。配置统一为 `relation.lifecycle-scope`，默认 default。清理构建后已确认制品不再包含 AssetEventTopics 或 ResolvedPath.Link。模型不授予权限，实际执行仍由来源控制；不新增 Provider 预检、推理、查询 DSL、事件运行时、可靠投递、常驻状态或逐事件追踪。事件仍沿原追踪和订阅链，无新增 MBean。

三次对照审查已完成：①按 7.3 的 SAREF、SOSA/SSN、PROV-O 与 OWL 边界区分领域概念、字段角色、值模型、规范关系及限定事实，不从共享概念推断数值等价，也不从 structural 推断传递性。②以真实 Expands 注解解析、动态模型写入、非设备来源及原设备发布器验证使用方式，保持未知词、来源执行和原 JSON 形状。③检查职责、异步适用性、并发追加、scope 装配和重复结构，删除主题重复展开及路径边复制，未增加新的匹配/授权/订阅 SPI。

本阶段集中回归按测试类去重共 206 项通过，0 失败/错误/跳过：core 62、components 60（公共 9、资产 7、关系 44）、本体 56、设备 28。覆盖概念注解/动态声明/合并冲突、开放关系分类、同端点不同记录、有效期和证据 JSON 往返、无关实体不登记异步任务、混合主题表示的并行追加/去重、提交/回滚、删除前快照、scope 清理及原权限/RPC。core/components/本体编译和 install 成功，设备 clean test 成功；11 个公共源文件 Javadoc doclint 无诊断。不是全模块全量测试，原服务、混合版本部署与宕机补发未验收。

复现命令和日志索引保存在隔离工作目录 `task-state.md`；最终日志为 `core-minimal-convergence-test.log`、`components-minimal-convergence-clean.log`、`ontology-minimal-convergence-clean.log`、`device-minimal-convergence-clean.log`。首轮新增测试的 JUnit 写法、分段集合比较及证据身份夹具已按真实契约修正；设备增量构建暴露旧子类字节码后改为干净构建。最终结果不累计失败重试或上阶段测试。core 基础语义已由 #114 合入 #112 分支，本节增量以其合并结果为基线单独评审；运行服务与浏览器由用户管理。

### 7.8 对象标识与字段引用

来源明确区分自身标识、业务编号、指向其他对象的字段和普通数据，消费者据此统一呈现；设备的产品、工单的项目和人员的部门使用相同契约。本节的值对象、注解、默认合并与前端消费已实现；设备来源完成最小声明，真实运行服务需重启加载。

实施前：`PropertySemanticRole` 的 identity、subjectId、modelId 等用途不能表达“这个字符串指向哪个类型的对象、对应哪条关系”。`DeviceObjectProvider` 原已声明设备到产品的 product 关系，但 productId、productName 只是普通字符串。现在通过 `PropertyReferenceMapping` 连接原关系，而不按字段名过滤或生成另一条关系。

依据：OWL 2 Primer 第 4.4/4.8 节区分对象之间的关系与对象到字面值的属性；第 4.4 节强调方向和无歧义关系名。这不要求平台把物模型改成 OWL，也不代表模型字段名就是领域语义。`semanticId` 仍引用概念，不引用某个实例；productId 也不能被自动解释为 rdf:type 或对象类型。

| 含义 | 建议契约 | 消费规则 |
| --- | --- | --- |
| 当前模型所述实体自身的身份 | 复用 semanticRole=identity | 当前对象以可信上下文中的 type/id 定位；名称优先，ID 为辅助信息。记录自己的 identity 仍属于记录，不挪到被描述设备的标题 |
| 资产编号、序列号等业务编号 | 可选增加 identifier 字段角色 | 作为业务信息保留；不自动认为唯一、不取代对象身份、不触发查询或关系生成 |
| 指向产品、部门、项目等对象 | 新增一个可选、类型化的字段引用说明 | 原值类型不变，字符串/数组仍沿原 DataType；对象名称、进入目标及关系消费使用统一引用处理 |
| 温度、说明、状态等数据 | 原字段角色、DataType 和展示声明 | 按原值类型渲染，不根据 Id 后缀或名称推测语义 |

原 `PropertyMetadata.expands` 增加可选 reference 声明，core 提供统一键、可序列化的 `PropertyReferenceMapping`、`MetadataSemantics.withReference/reference` 及组合注解 `@PropertyReference`。成员为 targetType、可选 relationId、reverse（默认 false）；可选 labelProperty 仅引用同层已公开字段，表示来源提供的对象名称快照。字段自身就是 ID 值的读取位置，不增加表达式、任意脚本、表连接或另一套 DataType。数组元素复用同一说明；本轮前端消费当前对象的顶层属性，嵌套结构继续沿原类型渲染。

静态声明示例，动态字段使用 `MetadataSemantics.withReference`：

```java
@SemanticRole(PropertySemanticRole.identity)
@Schema(description = "设备标识")
private String id;

@PropertyReference(targetType = TYPE_PRODUCT, relationId = REL_PRODUCT,
                   labelProperty = "productName")
@Schema(description = "所属产品")
private String productId;
```

TYPE_PRODUCT 与 REL_PRODUCT 引用来源已有常量；动态模型使用同一 helper 和编码，不能在前端再手写一份魔法值规则。`modelId` 等用途与 reference 正交：某字段可以同时表示所用模型和指向模型对象，但不能用 modelId 替代目标类型声明。目标名称优先取原来源已授权返回的 labelProperty；需要补充目标数据时继续由目标来源授权读取，名称快照不证明目标仍存在或可打开。

关系定义继续归 `RelationMetadata`，reference 只链接既有定义。完整关系定位包含定义所在对象类型、relationId、targetType 和方向，不按 relationId 全局合并；reverse 只是导航投影，不改变规范事实。未声明 relationId 时，只表示对象引用，不虚构“关联到”边。声明了关系时，概览与图谱复用同一来源的导航和事实，不再按字段创建第二条关系、不另建关系存储；数据行已有 `ObjectDataMetadata/ObjectLink` 的实例联系继续复用。

职责与边界：core 拥有语义值、注解、默认合并及最小结构读取；relation 负责把来源原声明和规范关系目录完整透传，复用原导航与授权；web-core 提供引用解析、紧凑对象呈现和统一值渲染；ontology-ui 决定标题、概览、关联图中的位置与默认去重。业务仅声明真实映射和必要的自定义表现，不注册针对产品 ID 的专用渲染器。引用不授予写权限，也不把外键变成可编辑输入框；维护仍使用来源已注册的关系维护器或明确动作。

解析仅处理当前可见内容，按可信 scope、类型和来源合并已有查询、请求内去重；切换主体取消旧请求，不新增全局跨账号缓存。未能解析目标时保留来源实际返回的可读标识，不凭空补名称或提供可打开承诺；来源未区分拒绝和不存在时统一显示不可查看。未声明名称映射的普通字段保留，不按 productName 等命名自动隐藏。默认布局消费声明后去重，用户保存的字段和布局不被改写。

落点：core 的 `PropertyReferenceMapping`、`PropertyReference` 和 `MetadataSemantics`；relation 保持原元数据透传；web-core 的 `object-capability/references.ts`、`useObjectCapabilities` 和 `ObjectReferenceDisplay`；本体 `ResourceOverview/ResourceBrowser` 负责布局及原导航。设备与产品自身标识声明 identity，设备 productId 指向原 product 关系并读取 productName 名称快照。不改变设备取数、关系维护或授权流程。

验收：注解与动态装配编码一致，JSON/Codec/RPC 及默认合并不丢失；identity、业务编号和 modelId 不被误识别；同 ID 不同类型/租户、正反向关系、多个来源、名称快照、长名称、单/多值、空值、目标无法读取及旧模型均有明确结果；概览引用、图谱边和进入目标保持同一身份与原授权；批量列表不产生逐字段查名称，未保存布局及原关系维护链不回退。阶段末集中编译、契约/集成测试和真实页面复验，不逐次构建。

验证：core 14 项测试通过，覆盖注解/动态装配、冲突合并、未知键保留、JSON 与 Java 序列化；relation 8 项测试通过，包含真实两节点 RPC 的引用声明往返；设备/产品 4 项来源回归通过。core 与设备制品已安装，运行服务加载及真实产品引用仍待用户重启后验证。前端数据、组件、构建与实验验收见本体交互设计；多租户源的实际数据隔离仍由原来源契约负责，本轮仅验证请求隔离与无全局缓存。实现提交：9449b994；[core PR #116](https://github.com/jetlinks/jetlinks-core/pull/116)。

参考：https://www.w3.org/TR/owl2-primer/#Object_Properties 、https://www.w3.org/TR/owl2-primer/#Datatypes 。

## 8. 错误、事件及集群

| 情况 | 处理 |
| --- | --- |
| 能力未实现/旧节点未声明支持 | 明确不支持，不当成成功空结果 |
| 支持查询但无可见记录 | 分页查询返回原空 PagerResult；其他读取沿用原空结果，保留来源隐藏不可见对象的规则 |
| 旧实例无完整模型 | getMetadata 为空，原属性与关系继续可用 |
| 参数、方向或筛选不合法 | 复用原校验/i18n 异常，不自动换其他关系/查询 |
| 权限、状态、网络或服务失败 | 保留原错误，不吞成空、成功或旧版本降级 |

不新增异常层级/结果包装。新增文案及模型展示沿用原国际化机制。来源逐次授权，通知接收人过滤不能由通用资产权限替代，关系可见性不能扩大命令权限。

事件通过 EventMetadata 发现，订阅复用 EventDefinitionProvider、EventSubscriptionManager 及其长期上下文、取消和投递机制；不在 core 新增事件运行时。定义未绑定订阅时不得宣称可订阅，ThingEvent 载荷也不自动等于持久发生记录。

已确认的事件契约补齐：core 只维护 EventKind、EventSemantics 注解及 MetadataSemantics 类型化访问，区分对象生命周期、关系定义、关系实例、对象数据和业务事件；开放的事件 ID 与来源 payload 仍由来源声明。relation 提供 ObjectEventReference 身份引用及 Spring 本地通知，业务 data 和跨服务信封继续使用 StructuredEvent。对象创建、保存、修改、删除分别表达，saved 不推断 created/modified，上报不推断值变化，关系实例保留规范 source/relation/target 和来源实例标识。

实现按 core 声明 → 公共资产发布复用 → relation 桥接及原删除消费者迁移 → 集中契约/事务/序列化回归推进。已存在的设备实体与设备消息结构化转发保持原 ID、事件类型、Topic、权限快照和订阅入口；对象语义在公共资产发布阶段附加，别名 Topic 复用同一事件，不再次创建设备业务事件。普通 ORM 身份来源才走兜底桥接。scope 继续隔离，删除前取得资产路由、提交后流转，回滚不发；内部异步句柄不进入线上数据。

范围为 core、components/common/assets/relation 及本体原删除消费；不扩大设备等业务事件接入，不迁移既有公开 Topic，不新增事件注册中心、可靠消息/outbox、代际标识、权限引擎或全量采集。追踪沿原事件总线和订阅机制，无新增常驻队列或 MBean。验收包含声明合并及开放词汇、三类载荷 JSON/Java 编码、本地与跨服务同 ID、提交/回滚、复用资产路由且不重复设备转发、非 GenericEntity 诊断、同 scope 删除清理与自定义非设备来源；真实服务和宕机恢复仍需部署验证。

事件补齐的三轮对照已完成：①沿 7.3 的规范边界区分身份、关系声明、实际关系和业务发生，保留 saved、同值上报及规范关系方向，不把来源标识当作因果证明。②以真实设备原发布器和非设备巡检样例核对接入，身份来源只需 ObjectEntity，补充来源仍使用原命令/模型；身份引用命名为 ObjectEventReference，业务载荷仍在 StructuredEvent.data。③核对 core 声明、资产快照/路由、relation 适配与本体清理各自职责，删除原单用途 ObjectLifecycleEvents，未增加 Provider 权限预检、缓存或第二套事件注册器。

本轮集中验证按测试类去重为 126 项（core 18、common 4、assets 6、relation 43、本体 27、设备 28），均无失败、错误或跳过；不是各模块全量测试。relation 最终 clean install 后 43 项通过，确认旧 ObjectLifecycleEvents/ObjectEventData 字节码不再进入制品；引用命名定稿后设备实体 6 项及本体消费者 3 项复验通过。14 个公共契约源文件 Javadoc doclint 无诊断。具体命令和日志索引保存在当前隔离工作目录的 task-state.md；原业务进程与真实集群部署未验收。

集群复用 ClusterRelationManager 和既有命令/RPC。远端只传模型、身份、关系、有界查询、功能 ID/参数及结果，不传 ObjectOperations、CommandSupport、回调或 Reactor Context。源端重新取得 operations(id)、绑定主体并授权。

认证、Trace、Monitor 引用沿原白名单传播，异步父子、聚合输入和重试依据仍由原记录/采集保存；本门面不重复包裹已观测执行，也不将业务发生记录等同于 Monitor span。

ThingMetadata 默认 toJson 已补 relations/modules。SimpleObjectType 保留旧 Externalizable 字段顺序和原制品 UID，不承载功能/事件/标签/模块；这些目录属于独立 ThingMetadata。完整模型与实例命令走独立 `relation-object-operations-v1` RPC，缺少该服务时保持旧属性/关系能力，新能力明确不支持，不将 at 降级到旧请求。

轻量入口无 I/O，属性读取不触发全部目录发现，元数据按需解析；不跨用户缓存授权。无常驻队列、订阅、后台扫描或独立 MBean，观测沿用原机制。性能验证关注旧路径及一次订阅的实际远程调用次数。

## 9. 实施计划与验收

已按确认方案实施，每个连贯阶段末集中验证。下表保留职责范围；首期的来源迁移以实际需要为限。

本轮按审查结论已完成以下优化，不扩大业务接入范围：

1. ontology-component 复用 relation 的有效资产授权入口，补真实存储下的收窄范围与重复订阅回归。
2. core 的组合物模型采用明确的实例优先扩展合并，保证单键 JSON 和未知词汇保留；不改变通用 CompositeMap 的其他使用方。
3. 在默认物模型 Codec 的 owning repository（jetlinks-supports）补齐可选关系/模块目录往返，收敛 relation 内重复解析；不复制完整 Codec，不为业务添加专用分支。
4. relation-component 仅整理查询阶段、复用模型装配和触及片段格式，保留订阅上下文、权限及旧 RPC 契约，不新增缓存、注册中心或执行层。

风险与验证：默认 Codec 属于独立仓库，core、supports 与 relation 必须配套构建/发布；旧 supports 不支持新增目录，不能仅升级 relation 后宣称完整模型传输。本地 supports 源码工作树为 `jetlinks-supports/`，基于上游 `1.3` 分支的 `a16007977666324f0a63f51234bd40eefe7b2d8f`，未加入根聚合模块或 `.gitmodules`，后续提交应归属 jetlinks-supports 仓库。模型/Codec、有效资产范围、关联分页、两节点 RPC 及原业务闭环已集中验证，见 9.3。运行服务原缺失的 tenant-notify 依赖仍单独记录。本轮不改前端、业务状态机或自动血缘采集范围。

| 阶段 | Owning module | 交付 |
| --- | --- | --- |
| 1 | jetlinks-core | ObjectOperations、metadata 包语义词汇/工具、RelationObject 兼容默认值；再集中验证通用 ThingMetadata 可选关系目录、独立模型获取和旧类型兼容。公共方法注释写清调用、空/错误、并发及副作用，@since 使用真实版本 |
| 2 | relation-component，必要时 common-component 原传输边界 | ObjectDataMetadata/ObjectLink 行协议值、ObjectData 元字段/访问器、独立查询请求、原分页、Provider 单向兼容、模型发现；落实 7.4 的路由、权限、分页、能力声明，模型与数据 Codec、集群协商及 at 不降级 |
| 3 | 原告警/通知来源与 ontology 现有扩展 | 通用来源能力放回原业务，业务移除 ontology 依赖；本体保留业务模板、证据及完成条件，收敛关系语义词汇 |
| 4 | 原业务集成边界 | 上报→场景→聚合告警→通知失败/重试→处置→效果核验；缺失输入/历史依据保持未知 |

必要验收：

1. 旧 Provider/RelationObject 原调用保持兼容，无适配递归；导航后模型及命令不丢失。入口无 I/O，订阅使用当前上下文。
2. 模型/原字段/功能对应真实能力；旧 ObjectType 实现不补新方法也保留原属性/关系，新功能默认值不伪造能力。temperature 与 property.temperature 的模型到读取路径可被正确消费，集合筛选不误用该路径；不同产品/旧版本不混用模型，产品/模板模型 ID 不被对象类型或实例 ID 覆盖。语义未知、冲突和新值不误分类，正反向仍表示同一关系。
3. 请求与结果贯通 Manager/Provider/集群，固定关系与平台管理关系各按权威来源读取；不支持受控对象分页的跨源组合明确拒绝，不能冒充正确总数。逐行验证 `$metadata` 的 type、原 ID、角色和匹配关系，行内反向标志以当前数据行为起点，不能照抄请求；未知语义不默认分类。OR、投影、计数和分页不越权，字段碰撞及非法元信息不被吞掉；旧接口字段形状不变，Map/类型化值及未知键往返保留，身份读取不遗留旧缓存。请求快照、at 支持/拒绝/旧节点路径不串对象或静默降级，不把元字段当历史/证据证明。
4. 命令输入不能覆盖主体，原授权和状态校验有效；显式 Command 与动态 ID 两种入口均按订阅读取当前上下文。单结果/流式、错误及取消保留；不添加自动重试，幂等只消费来源明确契约，任务受理、执行完成和业务核验分别判断，query 分类不掩盖业务副作用。
5. 两节点真实编解码覆盖模型、查询、命令、授权及 Observation 引用；通用模型及组合/合并视图的关系、语义不丢失，旧 Codec 不支持新字段时明确区分能力。模型包含循环关系时有限输出，不递归展开全部 relatedTypes。旧能力缺失与网络失败分开，不序列化活句柄。
6. 通知不依赖 ontology artifact 仍可构建并接入，接收人过滤保留；尝试 ID、聚合输入及核验分别保留，无 ID 遥测不伪造身份/血缘。
7. 首期公开的扩展键有明确形状、缺省及错误语义，语义词与标识不受语言影响；未知键/词往返保留，不能因 unknown 当作 false 或启用能力。动态模型校验能定位错误元素/路径，单位、窗口、版本和来源缺失不能核验通过。7.6 中未启动的扩展不作为首期交付承诺。

当前代码入口：core 的 things/ThingMetadata、things/relation、command；relation-component 的 RelationObjectProvider、DefaultRelationObject、DefaultRelationObjectManager、DefaultRelationOperation、ClusterRelationManager、ObjectQueryParam、RelatedEntity；device-manager 的 DeviceObjectProvider；things-component 的 ThingsDataRepository/QueryOperations；ontology-core 的 RelationQuery、RelationFact、OntologyObjectSource；ontology-rule 的 AlarmOccurrenceSource。

### 9.1 首期实际支持范围

| 入口/来源 | 已实现 | 明确边界 |
| --- | --- | --- |
| core / ObjectOperations | 实例身份、模型、属性、关系、同步命令句柄；语义扩展与可选关系目录 | 不增加 Thing 生命周期或数据存储 |
| 旧 Provider | 模型和属性单向适配；导航后保留新能力 | 不伪造命令、遥测模型或历史能力 |
| 来源固定关系 | 新请求完整传递、返回端声明字段校验、双端授权、规范方向行元信息 | 来源须覆盖新请求重载；不自动映射旧分页 |
| 平台管理关系 | 原定义/关联存储；目标 Provider 在 getType.expands 声明 `managedRelationQuery=true` 时使用同库 exists 查询 | 默认关闭；跨节点目标、跨库或历史查询不支持，不拉全量 ID 拼页 |
| 模型与命令 RPC | 原节点路由 + 独立版本服务；复用 CommandSupportRpcService 的单结果/流式、认证和现有上下文 Codec | 无动作自动重试；缺少服务不静默降级；at 传输不证明来源保存了历史 |
| 通知尝试 | notify-manager 的 NotificationAttemptObjectProvider 提供原记录读取与授权内计数；本体导出移入 ontology-notify | 仅当前用户的原投递记录；默认无命令；origin 不能证明 generated/triggered |
| 告警/设备等旧来源 | 保留原 Command/本体模板与事实证据读取，告警模型增加数据/字段角色 | 未将全部来源迁移到新关联分页或自动采集完整血缘 |

Manager 在方法调用时快照请求，每次订阅读取当前 Authentication 与有效 AssetsHolder；固定主体、用户 OR、资产范围分别组合。用户条件、排序、投影只能使用来源 `metadata()` 声明的集合字段（另保留 id），不能将属性读取路径或 `$metadata` 当作隐式查询字段。Manager 按所请求页返回数据，越界页为空，不沿用原 QueryHelper 夹回末页的行为。

### 9.2 后端接口

统一前缀 `/relation`，保留全部旧接口：

| 方法与路径 | 用途 |
| --- | --- |
| GET `/objects/{type}/metadata` | 类型模型与平台关系目录 |
| GET `/objects/{type}/{id}/metadata` | 实例适用模型；保留产品/模板模型 ID，合并平台适用声明 |
| POST `/objects/related/_query` | RelatedObjectQuery → PagerResult<ObjectData>，每行带 `$metadata` |
| GET `/objects/{type}/{id}/commands` | 实例命令目录 |
| POST `/objects/{type}/{id}/commands/{commandId}/_query` | 仅允许来源明确声明 functionKind=query 的功能，使用查询权限入口 |
| POST `/objects/{type}/{id}/commands/{commandId}` | 动作入口；还需来源执行原授权与状态校验 |

接口先检查主体可见性；绑定身份来自路径/句柄。来源必须约束原命令的主体参数，不公开整个业务命令服务。Java 内部取得句柄仍不代表已授权或对象存在。关联维护继续使用原 bind/unbind 服务，RelationOperation.save 明确返回不支持。

### 9.3 集中验证

2026-10-29，本轮收敛后共 91 项测试通过，本地已完成：

- core：42 项测试通过，包含 6 项模型/SPI 测试及 36 项原 Monitor/Recorder 回归，install 成功。组合模型按主模型优先合并扩展，无关扩展键数量不再改变语义；JSON 单键输出且保留未知词。
- supports：4 项测试通过，默认设备/通用 Codec 的完整模型编码往返、copy/merge/fromJson 缓存重载、单位与未知扩展，以及旧模型缺省目录均通过，install 成功。关系/模块按需解析，直接复用通用模型编码，删除 relation 的重复 ObjectMetadataCodec。
- relation：12 项测试通过，包含真实 H2/ORM 动态关系正反向、可见总数与 OR 隔离、请求快照、取消/错误不重试、两节点真实 RPC、实例模型合并平台关系目录、模型和 Map 元信息往返、旧节点拒绝新能力，install 成功。
- ontology：33 项测试通过，覆盖泵房/冷库闭环、告警通知边界、原通知 Worker/异步恢复、持久化、路径和证据，相关模块 install 成功。新增真实 H2/ORM 回归验证有效 Holder、OR 条件隔离、同一 Publisher 的不同范围重复订阅、双端可见性、活动 Scope 交集及缺失 Scope 上下文时拒绝。通知独立构建沿用前阶段成功证据，业务源无 ontology import/依赖。
- 依赖边界及格式：通用 SPI/关系组件无设备、告警、通知或本体业务 import；notify-manager 无 ontology import/依赖；相关仓库 `git diff --check` 通过。

主要命令：

```sh
mvn -o -f jetlinks-core/pom.xml -Dtest=MetadataSemanticsTest,ObjectOperationsTest,RecorderTest,FluxObservationTest,MonitorObservationTest -Dsurefire.failIfNoSpecifiedTests=false install
mvn -o -f jetlinks-supports/pom.xml -Dtest=MetadataDirectoryTest,DeviceMetadataCodecsTest -Dsurefire.failIfNoSpecifiedTests=false install
mvn -o -f modules/jetlinks-components/pom.xml -pl relation-component -Dtest=ObjectOperationsIntegrationTest,ObjectOperationsRpcTest,ObjectOperationsAdapterTest,ObjectDataContractTest,SameTypeRelationTest -Dsurefire.failIfNoSpecifiedTests=false install
mvn -o -f modules/notify-manager/pom.xml -DskipTests install
mvn -o -f modules/jetlinks-ontology/pom.xml -pl ontology-manager -am -Dtest=IoTBusinessClosureTest,SceneNotificationBoundaryTest,DeviceAlarmSourceIntegrationTest,OntologyPersistenceTest,RelationEvidenceTest,OntologyResolvedPathTest -Dsurefire.failIfNoSpecifiedTests=false install
```

运行服务装配尚未验证通过：`mvn -o -f runtime/notify-service/pom.xml -DskipTests compile` 在依赖解析阶段失败，本地缓存缺少原有的 `org.jetlinks.pro:tenant-notify:2.20.0-SNAPSHOT`，且当前工作树的 `modules/jetlinks-tenant` 未检出源码。`ontology-notify` 已随上述本体模块构建及闭环测试通过；补齐租户依赖后需重新执行该运行服务编译，并验证实际服务启动及跨服务装配。

H2 验证真实存储、查询和事务；双节点测试验证真实 RPC 编解码及认证。设备输入、AI 结果和短信/外部系统仍使用受控输入或 I/O 替身，未声称验证物理设备、算法精度、生产通道或精确输入血缘。七项官方规范的对照见 7.3，不包含最新论文的系统检索。提交/PR：pending。
