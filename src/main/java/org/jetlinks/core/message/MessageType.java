package org.jetlinks.core.message;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.firmware.*;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@AllArgsConstructor
public enum MessageType {

    UNKNOWN(null),
    //上报设备属性
    REPORT_PROPERTY(ReportPropertyMessage::new),

    //下行读写属性
    READ_PROPERTY(ReadPropertyMessage::new),
    WRITE_PROPERTY(WritePropertyMessage::new),
    //上行读写属性回复
    READ_PROPERTY_REPLY(ReadPropertyMessageReply::new),
    WRITE_PROPERTY_REPLY(WritePropertyMessageReply::new),
    //下行调用功能
    INVOKE_FUNCTION(FunctionInvokeMessage::new),
    //上行调用功能回复
    INVOKE_FUNCTION_REPLY(FunctionInvokeMessageReply::new),
    //事件消息
    EVENT(EventMessage::new),

    //广播,暂未支持
    BROADCAST(null),
    //设备上线
    ONLINE(DeviceOnlineMessage::new),
    //设备离线
    OFFLINE(DeviceOfflineMessage::new),

    //注册
    REGISTER(DeviceRegisterMessage::new),
    //注销
    UN_REGISTER(DeviceUnRegisterMessage::new),

    //平台主动断开连接
    DISCONNECT(DisconnectDeviceMessage::new),
    //断开回复
    DISCONNECT_REPLY(DisconnectDeviceMessageReply::new),

    //派生属性
    DERIVED_METADATA(DerivedMetadataMessage::new),

    //下行子设备消息
    CHILD(ChildDeviceMessage::new) {
        @Override
        @SuppressWarnings("all")
        public <T extends Message> T convert(Map<String, Object> map) {
            Object message = map.remove("childDeviceMessage");
            ChildDeviceMessage children = super.convert(map);
            if (message instanceof Map) {
                this.convertMessage(((Map<String, Object>) message))
                        .ifPresent(children::setChildDeviceMessage);
            }

            return (T) children;
        }
    },
    //上行子设备消息回复
    CHILD_REPLY(ChildDeviceMessageReply::new) {
        @Override
        @SuppressWarnings("all")
        public <T extends Message> T convert(Map<String, Object> map) {
            Object message = map.remove("childDeviceMessage");
            ChildDeviceMessageReply children = super.convert(map);
            if (message instanceof Map) {
                this.convertMessage(((Map<String, Object>) message))
                        .ifPresent(children::setChildDeviceMessage);
            }

            return (T) children;
        }
    },

    //读取设备固件信息
    READ_FIRMWARE(ReadFirmwareMessage::new),

    //读取设备固件信息回复
    READ_FIRMWARE_REPLY(ReadFirmwareMessageReply::new),

    //上报设备固件信息
    REPORT_FIRMWARE(ReportFirmwareMessage::new),

    //设备拉取固件信息
    REQUEST_FIRMWARE(RequestFirmwareMessage::new),
    //设备拉取固件信息响应
    REQUEST_FIRMWARE_REPLY(RequestFirmwareMessageReply::new),

    //更新设备固件
    UPGRADE_FIRMWARE(UpgradeFirmwareMessage::new),

    //更新设备固件信息回复
    UPGRADE_FIRMWARE_REPLY(UpgradeFirmwareMessageReply::new),

    //上报固件更新进度
    UPGRADE_FIRMWARE_PROGRESS(UpgradeFirmwareProgressMessage::new),

    //透传消息
    DIRECT(DirectDeviceMessage::new)
    ;

    Supplier<? extends Message> newInstance;

    @SuppressWarnings("all")
    public <T extends Message> T convert(Map<String, Object> map) {
        if (newInstance != null) {
            return (T) FastBeanCopier.copy(map, newInstance);
        }
        return null;
    }

    public static <T extends Message> Optional<T> convertMessage(Map<String, Object> map) {
        return of(map)
                .map(type -> type.convert(map));
    }

    public static Optional<MessageType> of(String name) {
        return Arrays.stream(MessageType.values())
                .filter(messageType -> messageType.name().equalsIgnoreCase(name))
                .findFirst();
    }

    public static Optional<MessageType> of(Map<String, Object> map) {
        Object msgType = map.get("messageType");
        if (msgType instanceof MessageType) {
            return Optional.of((MessageType) msgType);
        } else if (msgType instanceof String) {
            return of(((String) msgType));
        }

        if (map.containsKey("event")) {
            return Optional.of(EVENT);
        }

        if (map.containsKey("function")) {
            return map.containsKey("inputs") ? Optional.of(INVOKE_FUNCTION) : Optional.of(INVOKE_FUNCTION_REPLY);
        }

        if (map.containsKey("properties")) {
            Object properties = map.get("properties");
            return properties instanceof Collection ? Optional.of(READ_PROPERTY) : Optional.of(READ_PROPERTY_REPLY);
        }
        return Optional.empty();
    }

}