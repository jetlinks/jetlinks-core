package org.jetlinks.core.message;

/**
 * 广播消息
 */
public interface BroadcastMessage extends Message {

    String getAddress();

    Message getMessage();

    default MessageType getMessageType() {
        return MessageType.BROADCAST;
    }
}
