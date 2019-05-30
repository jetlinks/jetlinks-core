package org.jetlinks.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author bsetfeng
 * @version 1.0
 **/
@Getter
@AllArgsConstructor
public enum ErrorCode {
    /* 设备消息相关*/
    REQUEST_HANDLING("请求处理中"),
    CLIENT_OFFLINE("设备未在线"),
    NO_REPLY("设备未回复"),
    TIME_OUT("超时"),
    SYSTEM_ERROR("系统错误"),
    UNSUPPORTED_MESSAGE("不支持的消息"),
    PARAMETER_ERROR("参数错误"),
    PARAMETER_UNDEFINED("参数未定义"),
    FUNCTION_UNDEFINED("功能未定义"),
    PROPERTY_UNDEFINED("属性未定义")
    ;

    private String text;
}
