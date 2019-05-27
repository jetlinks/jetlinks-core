package org.jetlinks.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author bsetfeng
 * @version 1.0
 * @Date 2019/3/14 7:13 PM
 **/
@Getter
@AllArgsConstructor
public enum DeviceMessageType {

    FUNC("函数支持","func_invoke"),
    FUNC_REPLY("函数支持返回","func_invoke_reply"),
    READ_PROPERTY("读取属性","read_property"),
    READ_PROPERTY_REPLY("读取属性返回","read_property_reply"),
    ACK("回复类型","ack"),
    ;

    private String text;

    private String value;
}
