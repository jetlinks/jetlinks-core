package org.jetlinks.core.metadata.types;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;

import java.util.*;

@Getter
@Setter
public class EnumType extends AbstractType<EnumType> implements DataType {
    public static final String ID = "enum";

    private volatile List<Element> elements;

    private boolean multi;

    /**
     * 值类型
     */
    private DataType valueType;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "枚举";
    }

    public EnumType multi(boolean multi) {
        this.multi = multi;
        return this;
    }

    @Override
    public ValidateResult validate(Object value) {
        if (elements == null) {
            return ValidateResult.fail("值[" + value + "]不在枚举中");
        }
        for (Element ele : elements) {
            if (match(value, ele)) {
                //类型完全相同,则使用原始值作为对象.
                Object actValue = String.valueOf(value).equals(ele.value) ? value : ele.value;
                if (valueType instanceof Converter) {
                    actValue = ((Converter<?>) valueType).convert(actValue);
                }
                return ValidateResult.success(actValue);
            }
        }
        return ValidateResult.fail("值[" + value + "]不在枚举中");
    }

    private boolean match(Object value, Element ele) {
        if (value instanceof Map) {
            //适配map情况下的枚举信息
            @SuppressWarnings("all")
            Map<Object, Object> mapVal = ((Map<Object, Object>) value);
            return match(mapVal.getOrDefault("value", mapVal.get("id")), ele);
        }
        String strVal = String.valueOf(value);

        return Objects.equals(ele.value, strVal) || Objects.equals(ele.text, strVal);
    }

    @Override
    public String format(Object value) {
        String stringVal = String.valueOf(value);
        if (elements == null) {
            return stringVal;
        }
        return elements
            .stream()
            .filter(ele -> String.valueOf(value).equals(ele.value))
            .findFirst()
            .map(Element::getText)
            .orElse(stringVal);
    }

    public EnumType addElement(Element element) {
        if (elements == null) {
            synchronized (this) {
                if (elements == null) {
                    elements = new ArrayList<>();
                }
            }
        }
        elements.add(element);
        return this;
    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    public static class Element {
        private String value;

        private String text;

        private String description;


        public static Element of(String value, String text) {
            return of(value, text, null);
        }

        public static Element of(Map<String, String> map) {
            return Element.of(map.get("value"), map.get("text"), map.get("description"));
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("value", value);
            map.put("text", text);
            map.put("description", description);

            return map;
        }
    }
}
