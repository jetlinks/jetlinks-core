package org.jetlinks.core.collector;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
public enum AccessMode implements I18nEnumDict<String> {
    read("读"),
    write("写"),
    subscribe("订阅");

    public static final AccessMode[] values = AccessMode.values();

    private final String text;

    @Override
    public String getValue() {
        return name();
    }

}