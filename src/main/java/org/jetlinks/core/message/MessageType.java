package org.jetlinks.core.message;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@AllArgsConstructor
public enum MessageType {

    UNKNOWN(null),

    READ_PROPERTY(ReadPropertyMessage::new),
    WRITE_PROPERTY(WritePropertyMessage::new),

    READ_PROPERTY_REPLY(ReadPropertyMessageReply::new),
    WRITE_PROPERTY_REPLY(WritePropertyMessageReply::new),
    INVOKE_FUNCTION(FunctionInvokeMessage::new),
    INVOKE_FUNCTION_REPLY(FunctionInvokeMessageReply::new),
    EVENT(EventMessage::new),
    BROADCAST(null),
    ONLINE(DeviceOnlineMessage::new),
    OFFLINE(DeviceOfflineMessage::new),
    DISCONNECT(DisconnectDeviceMessage::new),
    DISCONNECT_REPLY(DisconnectDeviceMessageReply::new),
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