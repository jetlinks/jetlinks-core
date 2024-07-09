enum MessageType{
    REPORT_PROPERTY,
    READ_PROPERTY,
    WRITE_PROPERTY,
    READ_PROPERTY_REPLY,
    WRITE_PROPERTY_REPLY,
    INVOKE_FUNCTION,
    INVOKE_FUNCTION_REPLY,
    EVENT,
    BROADCAST,
    ONLINE,
    //设备离线
    OFFLINE,
    //注册
    REGISTER,
    //注销
    UN_REGISTER,

    //平台主动断开连接
    DISCONNECT,
    //断开回复
    DISCONNECT_REPLY,

    //派生属性
    DERIVED_METADATA,

    //下行子设备消息
    CHILD,
    //上行子设备消息回复
    CHILD_REPLY,

    //读取设备固件信息
    READ_FIRMWARE,

    //读取设备固件信息回复
    READ_FIRMWARE_REPLY,

    //上报设备固件信息
    REPORT_FIRMWARE,

    //设备拉取固件信息
    REQUEST_FIRMWARE,
    //设备拉取固件信息响应
    REQUEST_FIRMWARE_REPLY,

    //更新设备固件
    UPGRADE_FIRMWARE,

    //更新设备固件信息回复
    UPGRADE_FIRMWARE_REPLY,

    //上报固件更新进度
    UPGRADE_FIRMWARE_PROGRESS,

    //透传消息
    DIRECT,

    //更新标签
    //since 1.1.2
    UPDATE_TAG,

    //日志
    //since 1.1.4
    LOG,

    //应答指令
    ACKNOWLEDGE,

    //状态检查
    STATE_CHECK,
    STATE_CHECK_REPLY,

    //数采数据上报消息
    REPORT_COLLECTOR,
    READ_COLLECTOR_DATA,
    READ_COLLECTOR_DATA_REPLY,
    WRITE_COLLECTOR_DATA,
    WRITE_COLLECTOR_DATA_REPLY,

    BATCH,
    //未知消息
    UNKNOWN
}

export {MessageType}