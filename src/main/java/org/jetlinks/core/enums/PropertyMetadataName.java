package org.jetlinks.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PropertyMetadataName {

    UNIT("单位","unit"),
    DESCRIPTION("说明","description"),
    FORMAT("单位","format"),
    ACCESS("使用类型","access"),
    RANGE("范围","range"),
    TYPE("urn表达式","type"),
    MAX_LENGTH("最大长度","max-length")
    ;

    private String text;

    private String value;
}
