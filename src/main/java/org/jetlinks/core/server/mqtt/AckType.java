package org.jetlinks.core.server.mqtt;

public enum AckType {
    //QoS1 确认收到消息
    PUBACK,
    //QoS 2 消息以接收
    PUBREC,
    //QoS 2 释放
    PUBREL,
    //Qos 2 确认处理完成
    PUBCOMP
}
