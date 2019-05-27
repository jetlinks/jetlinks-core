package org.jetlinks.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventMetadataName {

    DESCRIPTION("说明","description"),
    ARGUMENTS("被变更的参数","arguments")
    ;

    private String text;

    private String value;
}
