package org.jetlinks.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProtocolType {

    DEFAULT("jet-links","0  default"),
    OTHER("other","1");

    private String text;

    private String value;
}
