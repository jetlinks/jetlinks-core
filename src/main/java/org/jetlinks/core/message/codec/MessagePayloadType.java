package org.jetlinks.core.message.codec;

public enum MessagePayloadType {

    JSON, STRING, BINARY, HEX, UNKNOWN;

    public static MessagePayloadType of(String of) {
        for (MessagePayloadType value : MessagePayloadType.values()) {
            if (value.name().equalsIgnoreCase(of)) {
                return value;
            }
        }
        return UNKNOWN;
    }
}
