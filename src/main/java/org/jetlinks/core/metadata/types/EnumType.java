package org.jetlinks.core.metadata.types;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class EnumType implements DataType {
    public static final String ID = "enum";

    private volatile List<Element> elements;

    private String description;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "枚举";
    }

    @Override
    public ValidateResult validate(Object value) {
        if (elements == null) {
            return ValidateResult.fail("值不在枚举中");
        }
        return elements
                .stream()
                .filter(ele -> ele.value.equals(String.valueOf(value)))
                .findFirst()
                .map(e -> ValidateResult.success())
                .orElseGet(() -> ValidateResult.fail("值不在枚举中"));
    }

    @Override
    public String format(Object value) {
        String stringVal = String.valueOf(value);
        if (elements == null) {
            return stringVal;
        }
        return elements
                .stream()
                .filter(ele -> ele.value.equals(String.valueOf(value)))
                .findFirst()
                .map(Element::getText)
                .orElse(stringVal);
    }

    public void addElement(Element element) {
        if (elements == null) {
            synchronized (this) {
                if (elements == null) {
                    elements = new ArrayList<>();
                }
            }
        }
        elements.add(element);
    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    public static class Element {
        private String value;

        private String text;

        public static Element of(Map<String, String> map) {
            return Element.of(map.get("value"), map.get("text"));
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("value", value);
            map.put("text", text);

            return map;
        }
    }
}
