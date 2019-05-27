package org.jetlinks.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ServiceMetadataName {

    DESCRIPTION("说明","description"),
    PROPERTIES("属性集合","properties"),
    FUNCTIONS("功能集合","functions"),
    EVENTS("事件集合","events")
    ;

    private String text;

    private String value;
}
