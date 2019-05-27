package org.jetlinks.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FunctionMetadataName {

    DESCRIPTION("说明","description"),
    IS_ASYNC("异步标识","async"),
    INPUTS("输入参数","ins"),
    OUTPUT("输出参数","out"),
    ;

    private String text;

    private String value;
}
