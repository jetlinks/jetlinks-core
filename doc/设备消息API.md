# DeviceMessage API 文档

## 概述

`DeviceMessage` 是 JetLinks 平台中设备消息的核心接口，用于定义设备与平台之间交互的消息格式。它继承自 `ThingMessage`
，提供了设备消息的基本属性和方法。

注意：请勿自行实现DeviceMessage，请使用平台内部定义的相关消息类型，如：根据情况设计选择使用属性上报，功能调用或者事件上报。

## 核心接口

### DeviceMessage

```java
public interface DeviceMessage extends ThingMessage, Jsonable {
    // 设备ID
    String getDeviceId();

    // 时间戳,
    long getTimestamp();

    // 添加消息头
    DeviceMessage addHeader(String header, Object value);

    // 如果消息头不存在则添加
    DeviceMessage addHeaderIfAbsent(String header, Object value);

    // 复制消息
    DeviceMessage copy();
}
```

### DeviceMessageReply

```java
public interface DeviceMessageReply extends DeviceMessage, ThingMessageReply {
    // 是否成功
    boolean isSuccess();

    // 业务码，具体由设备定义
    String getCode();

    // 错误消息
    String getMessage();

    // 设置失败
    DeviceMessageReply error(ErrorCode errorCode);

    // 设置失败异常
    DeviceMessageReply error(Throwable err);

    // 设置设备ID
    DeviceMessageReply deviceId(String deviceId);

    // 设置成功
    DeviceMessageReply success();

    // 设置业务码
    DeviceMessageReply code(String code);

    // 设置消息
    DeviceMessageReply message(String message);

    // 根据另外的消息填充对应属性
    DeviceMessageReply from(Message message);

    // 设置消息ID
    DeviceMessageReply messageId(String messageId);

    // 设置时间戳
    DeviceMessageReply timestamp(long timestamp);
}
```

## 消息类型

JetLinks 平台支持多种设备消息类型，通过 `MessageType` 枚举定义：

### 属性相关消息

| 消息类型                   | 描述     | 方向      | Java类型                      |
|------------------------|--------|---------|-----------------------------|
| `REPORT_PROPERTY`      | 上报设备属性 | 设备 → 平台 | `ReportPropertyMessage`     |
| `READ_PROPERTY`        | 读取设备属性 | 平台 → 设备 | `ReadPropertyMessage`       |
| `WRITE_PROPERTY`       | 修改设备属性 | 平台 → 设备 | `WritePropertyMessage`      |
| `READ_PROPERTY_REPLY`  | 读取属性回复 | 设备 → 平台 | `ReadPropertyMessageReply`  |
| `WRITE_PROPERTY_REPLY` | 修改属性回复 | 设备 → 平台 | `WritePropertyMessageReply` |

### 功能调用相关消息

| 消息类型                    | 描述     | 方向      | Java类型                       |
|-------------------------|--------|---------|------------------------------|
| `INVOKE_FUNCTION`       | 调用设备功能 | 平台 → 设备 | `FunctionInvokeMessage`      |
| `INVOKE_FUNCTION_REPLY` | 调用功能回复 | 设备 → 平台 | `FunctionInvokeMessageReply` |

### 事件相关消息

| 消息类型    | 描述     | 方向      | Java类型         |
|---------|--------|---------|----------------|
| `EVENT` | 设备事件消息 | 设备 → 平台 | `EventMessage` |

### 设备状态相关消息

| 消息类型                | 描述       | 方向      | Java类型                         |
|---------------------|----------|---------|--------------------------------|
| `ONLINE`            | 设备上线     | 设备 → 平台 | `DeviceOnlineMessage`          |
| `OFFLINE`           | 设备离线     | 设备 → 平台 | `DeviceOfflineMessage`         |
| `REGISTER`          | 设备注册     | 设备 → 平台 | `DeviceRegisterMessage`        |
| `UN_REGISTER`       | 设备注销     | 设备 → 平台 | `DeviceUnRegisterMessage`      |
| `DISCONNECT`        | 平台主动断开连接 | 平台 → 设备 | `DisconnectDeviceMessage`      |
| `DISCONNECT_REPLY`  | 断开连接回复   | 设备 → 平台 | `DisconnectDeviceMessageReply` |
| `STATE_CHECK`       | 状态检查     | 平台 → 设备 | `DeviceStateCheckMessage`      |
| `STATE_CHECK_REPLY` | 状态检查回复   | 设备 → 平台 | `DeviceStateCheckMessageReply` |

### 固件相关消息

| 消息类型                        | 描述       | 方向      | Java类型                           |
|-----------------------------|----------|---------|----------------------------------|
| `READ_FIRMWARE`             | 读取设备固件信息 | 平台 → 设备 | `ReadFirmwareMessage`            |
| `READ_FIRMWARE_REPLY`       | 读取固件信息回复 | 设备 → 平台 | `ReadFirmwareMessageReply`       |
| `REPORT_FIRMWARE`           | 上报设备固件信息 | 设备 → 平台 | `ReportFirmwareMessage`          |
| `REQUEST_FIRMWARE`          | 设备拉取固件信息 | 设备 → 平台 | `RequestFirmwareMessage`         |
| `REQUEST_FIRMWARE_REPLY`    | 拉取固件信息响应 | 平台 → 设备 | `RequestFirmwareMessageReply`    |
| `UPGRADE_FIRMWARE`          | 更新设备固件   | 平台 → 设备 | `UpgradeFirmwareMessage`         |
| `UPGRADE_FIRMWARE_REPLY`    | 更新固件回复   | 设备 → 平台 | `UpgradeFirmwareMessageReply`    |
| `UPGRADE_FIRMWARE_PROGRESS` | 上报固件更新进度 | 设备 → 平台 | `UpgradeFirmwareProgressMessage` |

### 其他消息类型

| 消息类型               | 描述      | 方向      | Java类型                     |
|--------------------|---------|---------|----------------------------|
| `DIRECT`           | 透传消息    | 双向      | `DirectDeviceMessage`      |
| `UPDATE_TAG`       | 更新标签    | 平台 → 设备 | `UpdateTagMessage`         |
| `LOG`              | 设备日志    | 设备 → 平台 | `DeviceLogMessage`         |
| `ACKNOWLEDGE`      | 应答指令    | 设备 → 平台 | `AcknowledgeDeviceMessage` |
| `CHILD`            | 子设备消息   | 平台 → 设备 | `ChildDeviceMessage`       |
| `CHILD_REPLY`      | 子设备消息回复 | 设备 → 平台 | `ChildDeviceMessageReply`  |
| `DERIVED_METADATA` | 派生属性    | 平台 → 设备 | `DerivedMetadataMessage`   |
| `BATCH`            | 批量消息    | 双向      | `BatchMessage`             |
| `MODULE`           | 模块消息    | 双向      | `DeviceModuleMessage`      |

## 属性相关消息

### ReadPropertyMessage

读取设备属性消息，方向：平台 → 设备

```java
public class ReadPropertyMessage extends CommonDeviceMessage<ReadPropertyMessage>
    implements RepayableDeviceMessage<ReadPropertyMessageReply>, ReadThingPropertyMessage<ReadPropertyMessageReply> {

    // 要读取的属性列表
    private List<String> properties = new ArrayList<>();

    // 添加要读取的属性
    public ReadPropertyMessage addProperties(List<String> properties);

    public ReadPropertyMessage addProperties(String... properties);

    // 创建回复消息
    public ReadPropertyMessageReply newReply();
}
```

### ReadPropertyMessageReply

读取属性回复消息，方向：设备 → 平台

```java
public class ReadPropertyMessageReply extends CommonDeviceMessageReply<ReadPropertyMessageReply>
    implements ReadThingPropertyMessageReply {

    // 属性值
    private Map<String, Object> properties = new HashMap<>();

    // 设置属性值
    public ReadPropertyMessageReply addProperty(String property, Object value);

    public ReadPropertyMessageReply setProperties(Map<String, Object> properties);
}
```

### WritePropertyMessage

修改设备属性消息，方向：平台 → 设备

```java
public class WritePropertyMessage extends CommonDeviceMessage<WritePropertyMessage>
    implements RepayableDeviceMessage<WritePropertyMessageReply>, WriteThingPropertyMessage<WritePropertyMessageReply> {

    // 要修改的属性
    private Map<String, Object> properties = new HashMap<>();

    // 设置属性值
    public WritePropertyMessage addProperty(String property, Object value);

    public WritePropertyMessage setProperties(Map<String, Object> properties);

    // 创建回复消息
    public WritePropertyMessageReply newReply();
}
```

### WritePropertyMessageReply

修改属性回复消息，方向：设备 → 平台

```java
public class WritePropertyMessageReply extends CommonDeviceMessageReply<WritePropertyMessageReply>
    implements WriteThingPropertyMessageReply {

    // 属性值
    private Map<String, Object> properties = new HashMap<>();

    // 设置属性值
    public WritePropertyMessageReply addProperty(String property, Object value);

    public WritePropertyMessageReply setProperties(Map<String, Object> properties);
}
```

### ReportPropertyMessage

上报设备属性消息，方向：设备 → 平台

```java
public class ReportPropertyMessage extends CommonDeviceMessage<ReportPropertyMessage>
    implements ThingReportPropertyMessage {

    // 属性值
    private Map<String, Object> properties = new HashMap<>();

    // 设置属性值
    public ReportPropertyMessage addProperty(String property, Object value);

    public ReportPropertyMessage setProperties(Map<String, Object> properties);
}
```

## 功能调用相关消息

### FunctionInvokeMessage

调用设备功能消息，方向：平台 → 设备

```java
public class FunctionInvokeMessage extends CommonDeviceMessage<FunctionInvokeMessage>
    implements RepayableDeviceMessage<FunctionInvokeMessageReply>, ThingFunctionInvokeMessage<FunctionInvokeMessageReply> {

    // 功能ID
    private String functionId;

    // 输入参数
    private List<FunctionParameter> inputs = new ArrayList<>();

    // 设置功能ID
    public FunctionInvokeMessage functionId(String id);

    // 添加输入参数
    public FunctionInvokeMessage addInput(FunctionParameter parameter);

    public FunctionInvokeMessage addInput(String name, Object value);

    public FunctionInvokeMessage addInputs(Map<String, Object> parameters);

    // 创建回复消息
    public FunctionInvokeMessageReply newReply();
}
```

### FunctionInvokeMessageReply

调用功能回复消息，方向：设备 → 平台

```java
public class FunctionInvokeMessageReply extends CommonDeviceMessageReply<FunctionInvokeMessageReply>
    implements ThingFunctionInvokeMessageReply {

    // 功能ID
    private String functionId;

    // 输出结果
    private Object output;

    // 设置功能ID
    public FunctionInvokeMessageReply functionId(String functionId);

    // 设置输出结果
    public FunctionInvokeMessageReply output(Object output);
}
```

## 事件相关消息

### EventMessage

设备事件消息，方向：设备 → 平台

```java
public class EventMessage extends CommonDeviceMessage<EventMessage> implements ThingEventMessage {

    // 事件ID
    private String event;

    // 事件数据
    private Object data;

    // 设置事件ID
    public EventMessage event(String event);

    // 设置事件数据
    public EventMessage data(Object data);
}
```

## 使用示例

### 创建并发送读取属性回复消息

[//]: # (@formatter:off)

```java

// 处理回复
ReadPropertyMessageReply reply = new ReadPropertyMessageReply();
reply.addProperty("temperature",25.5);
reply.addProperty("humidity",60);
reply.success();

// 发送消息...

```

### 创建并发送上报属性消息

```java
// 创建消息
ReportPropertyMessage message = new ReportPropertyMessage();
message.setDeviceId("device-1");
message.addProperty("temperature",26.5);
message.addProperty("humidity",65);
message.setTimestamp(System.currentTimeMillis());

// 发送消息...
```

## 消息编解码

JetLinks 平台提供了消息编解码接口，用于将设备消息转换为平台可识别的格式：

```java
public interface DeviceMessageCodec {
    // 编码消息
    EncodedMessage encode(MessageEncodeContext context);

    // 解码消息
    DeviceMessage decode(MessageDecodeContext context);
}
```