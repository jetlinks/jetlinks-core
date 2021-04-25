package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.firmware.*;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.*;
import org.jetlinks.core.message.state.DeviceStateCheckMessage;
import org.jetlinks.core.message.state.DeviceStateCheckMessageReply;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@AllArgsConstructor
public enum MessageType {

    //上报设备属性
    REPORT_PROPERTY(ReportPropertyMessage::new),

    //下行读写属性
    READ_PROPERTY(ReadPropertyMessage::new),
    WRITE_PROPERTY(WritePropertyMessage::new),
    //上行读写属性回复
    READ_PROPERTY_REPLY(ReadPropertyMessageReply::new),
    WRITE_PROPERTY_REPLY(WritePropertyMessageReply::new),
    //下行调用功能
    INVOKE_FUNCTION(FunctionInvokeMessage::new) {
        @Override
        public <T extends Message> T convert(Map<String, Object> map) {
            Object inputs = map.get("inputs");
            //处理以Map形式传入参数的场景
            if (inputs instanceof Map) {
                Map<String,Object> newMap = new HashMap<>(map);
                @SuppressWarnings("unchecked")
                Map<String, Object> inputMap = (Map<String, Object>) newMap.remove("inputs");
                FunctionInvokeMessage message = super.convert(newMap);
                inputMap.forEach(message::addInput);
                return (T) message;
            }
            return super.convert(map);
        }
    },
    //上行调用功能回复
    INVOKE_FUNCTION_REPLY(FunctionInvokeMessageReply::new),
    //事件消息
    EVENT(EventMessage::new),

    //广播,暂未支持
    BROADCAST(DefaultBroadcastMessage::new),
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
    DIRECT(DirectDeviceMessage::new),

    //更新标签
    //since 1.1.2
    UPDATE_TAG(UpdateTagMessage::new),

    //日志
    //since 1.1.4
    LOG(DeviceLogMessage::new),

    //应答指令
    ACKNOWLEDGE(AcknowledgeDeviceMessage::new),

    //状态检查
    STATE_CHECK(DeviceStateCheckMessage::new),
    STATE_CHECK_REPLY(DeviceStateCheckMessageReply::new),

    //未知消息
    UNKNOWN(null) {
        @Override
        @SuppressWarnings("all")
        public <T extends Message> T convert(Map<String, Object> map) {
            if (map.containsKey("success")) {
                CommonDeviceMessageReply<?> reply = new ChildDeviceMessageReply();
                reply.fromJson(new JSONObject(map));
                return (T) reply;
            }
            CommonDeviceMessage reply = new CommonDeviceMessage();
            reply.fromJson(new JSONObject(map));
            return (T) reply;

        }
    };

    Supplier<? extends Message> newInstance;

    private static final Map<String, MessageType> mapping;

    static {
        mapping = new HashMap<>();
        for (MessageType value : values()) {
            mapping.put(value.name().toLowerCase(), value);
            mapping.put(value.name().toUpperCase(), value);
        }
    }

    @SuppressWarnings("all")
    public <T extends Message> T convert(Map<String, Object> map) {
        if (newInstance != null) {
            try {
                return (T) FastBeanCopier.copy(map, newInstance);
            } catch (Throwable e) {
                //fallback jsonobject
                return (T) new JSONObject(map).toJavaObject(newInstance.get().getClass());
            }
        }
        return null;
    }

    public static <T extends Message> Optional<T> convertMessage(Map<String, Object> map) {
        return of(map)
                .map(type -> type.convert(map));
    }

    public static Optional<MessageType> of(String name) {
        return Optional.ofNullable(mapping.get(name));
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

        if (map.containsKey("functionId")) {
            return map.containsKey("inputs") ? Optional.of(INVOKE_FUNCTION) : Optional.of(INVOKE_FUNCTION_REPLY);
        }
        if (map.containsKey("properties")) {
            Object properties = map.get("properties");
            return properties instanceof Collection ? Optional.of(READ_PROPERTY) : Optional.of(READ_PROPERTY_REPLY);
        }
        if (map.containsKey("tags")) {
            return Optional.of(UPDATE_TAG);
        }
        if (map.containsKey("success")) {
            return Optional.of(ACKNOWLEDGE);
        }
        return Optional.of(UNKNOWN);
    }

}