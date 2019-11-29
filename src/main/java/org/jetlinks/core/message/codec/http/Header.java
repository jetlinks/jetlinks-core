package org.jetlinks.core.message.codec.http;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Header {
    private String name;

    private String[] value;

    private String firstValue() {
        return (value != null && value.length > 0) ? value[0] : null;
    }
}