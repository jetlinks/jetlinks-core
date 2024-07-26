package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import org.jetlinks.core.device.DeviceThingType;
import org.jetlinks.core.message.collector.*;
import org.jetlinks.core.message.event.DefaultEventMessage;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.firmware.*;
import org.jetlinks.core.message.function.DefaultFunctionInvokeMessage;
import org.jetlinks.core.message.function.DefaultFunctionInvokeMessageReply;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.module.DefaultThingModuleMessage;
import org.jetlinks.core.message.module.DeviceModuleMessage;
import org.jetlinks.core.message.property.*;
import org.jetlinks.core.message.state.DeviceStateCheckMessage;
import org.jetlinks.core.message.state.DeviceStateCheckMessageReply;
import org.jetlinks.core.things.ThingId;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public enum MessageType {

    //上报设备属性
    REPORT_PROPERTY(ReportPropertyMessage::new, DefaultReportPropertyMessage::new),

    //下行读写属性
    READ_PROPERTY(ReadPropertyMessage::new, DefaultReadPropertyMessage::new),
    WRITE_PROPERTY(WritePropertyMessage::new, DefaultWritePropertyMessage::new),
    //上行读写属性回复
    READ_PROPERTY_REPLY(ReadPropertyMessageReply::new, DefaultReadPropertyMessageReply::new),
    WRITE_PROPERTY_REPLY(WritePropertyMessageReply::new, DefaultWritePropertyMessageReply::new),
    //下行调用功能
    INVOKE_FUNCTION(FunctionInvokeMessage::new, DefaultFunctionInvokeMessage::new),
    //上行调用功能回复
    INVOKE_FUNCTION_REPLY(FunctionInvokeMessageReply::new, DefaultFunctionInvokeMessageReply::new),
    //事件消息
    EVENT(EventMessage::new, DefaultEventMessage::new),

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
    CHILD(ChildDeviceMessage::new),
    //上行子设备消息回复
    CHILD_REPLY(ChildDeviceMessageReply::new),

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
    UPDATE_TAG(UpdateTagMessage::new, DefaultUpdateTingTagsMessage::new),

    //日志
    //since 1.1.4
    LOG(DeviceLogMessage::new),

    //应答指令
    ACKNOWLEDGE(AcknowledgeDeviceMessage::new),

    //状态检查
    STATE_CHECK(DeviceStateCheckMessage::new),
    STATE_CHECK_REPLY(DeviceStateCheckMessageReply::new),

    //数采数据上报消息
    REPORT_COLLECTOR(ReportCollectorDataMessage::new),
    READ_COLLECTOR_DATA(ReadCollectorDataMessage::new),
    READ_COLLECTOR_DATA_REPLY(ReadCollectorDataMessageReply::new),
    WRITE_COLLECTOR_DATA(WriteCollectorDataMessage::new),
    WRITE_COLLECTOR_DATA_REPLY(WriteCollectorDataMessageReply::new),

    BATCH(BatchMessage::new),
    MODULE(DeviceModuleMessage::new, DefaultThingModuleMessage::new),
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

    final Supplier<? extends Message> deviceInstance;
    final Supplier<? extends Message> thingInstance;

    private static final Map<String, MessageType> mapping;

    static {
        mapping = new HashMap<>();
        for (MessageType value : values()) {
            mapping.put(value.name().toLowerCase(), value);
            mapping.put(value.name().toUpperCase(), value);
        }
    }

    MessageType(Supplier<Message> deviceInstance) {
        this(deviceInstance, null);
    }

    MessageType(Supplier<? extends Message> deviceInstance, Supplier<? extends ThingMessage> thingInstance) {
        this.deviceInstance = deviceInstance;
        this.thingInstance = thingInstance;
    }

    public <T extends DeviceMessage> T forDevice() {
        if (deviceInstance == null) {
            return null;
        }
        Message msg = deviceInstance.get();
        if (msg instanceof DeviceMessage) {
            return (T) msg;
        }
        return null;
    }

    public boolean iSupportDevice() {
        return deviceInstance != null;
    }

    public <T extends ThingMessage> T forThing() {
        if (null == thingInstance) {
            return null;
        }
        return (T) thingInstance.get();
    }

    public <T extends ThingMessage> T forThing(ThingId thingId) {
        return forThing(thingId.getType(), thingId.getId());
    }

    public <T extends ThingMessage> T forThing(String type, String id) {
        T thing;
        if (!DeviceThingType.device.name().equals(type)) {
            thing = this.forThing();
        } else {
            thing = this.forDevice();
        }
        if (thing != null) {
            return (T) thing.thingId(type, id);
        }
        return null;
    }

    @SuppressWarnings("all")
    public <T extends Message> T convert(Map<String, Object> map) {
        Supplier<? extends Message> supplier = deviceInstance;

        if (deviceInstance != null) {
            if (thingInstance != null) {
                //不是设备
                Object type = map.get("thingType");
                if (type != null) {
                    if (!DeviceThingType.device.name().equals(type)) {
                        supplier = thingInstance;
                    }
                }
            }
            T msg = (T) supplier.get();
            msg.fromJson(new JSONObject(map));
            return msg;
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

    static final MessageType[] types = values();

    @SneakyThrows
    public static Message readExternal(ObjectInput input) {
        int type = input.readByte();
        if (type >= types.length) {
            return null;
        }
        MessageType messageType = types[type];
        boolean isDevice = input.readBoolean();
        Message message;
        if (isDevice && messageType.deviceInstance != null) {
            message = messageType.deviceInstance.get();
        } else if (messageType.thingInstance != null) {
            message = messageType.thingInstance.get();
        } else {
            message = new CommonDeviceMessage();
        }
        message.readExternal(input);
        return message;
    }

    @SneakyThrows
    public static void writeExternal(Message message, ObjectOutput output) {
        output.writeByte(message.getMessageType().ordinal());
        output.writeBoolean(message instanceof DeviceMessage);
        message.writeExternal(output);
    }
}