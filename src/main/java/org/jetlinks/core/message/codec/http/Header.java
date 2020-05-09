package org.jetlinks.core.message.codec.http;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Header {
    private String name;

    private String[] value;

    private String firstValue() {
        return (value != null && value.length > 0) ? value[0] : null;
    }
}