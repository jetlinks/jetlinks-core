package org.jetlinks.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeviceMetadataName {

    DESCRIPTION("说明","description"),
    SERVICES("服务集合","services")
    ;

    private String text;

    private String value;
}
