package org.jetlinks.core.metadata.types;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;

import java.util.*;
import java.util.stream.Collectors;

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
        return LocaleUtils.resolveMessage("data.type." + getId(), LocaleUtils.current(), "枚举");
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
        Object _value;
        if (multi) {
            _value = convertMulti(value);
        } else {
            _value = convert(value);
        }
        if (_value == null) {
            return ValidateResult.fail("值[" + value + "]不在枚举中");
        }
        return ValidateResult.success(_value);
    }

    private Object convert(Object value) {
        for (Element ele : elements) {
            if (match(value, ele)) {
                //类型完全相同,则使用原始值作为对象.
                Object actValue = String.valueOf(value).equals(ele.value) ? value : ele.value;
                if (valueType instanceof Converter) {
                    actValue = ((Converter<?>) valueType).convert(actValue);
                }
                return actValue;
            }
        }
        return null;
    }

    public Object convertMulti(Object value) {
        List<Object> _value = new ArrayList<>();
        List<Object> objects = toArray(value);
        if (!objects.isEmpty()) {
            for (Object object : objects) {
                Object _v = convert(object);
                if (_v != null) {
                    _value.add(_v);
                }
            }
        }
        if (_value.isEmpty()) {
            return null;
        }
        return _value;
    }

    private static List<Object> toArray(Object value) {
        List<Object> values = new ArrayList<>();
        if (value instanceof Collection) {
            values = new ArrayList<>(((Collection<?>) value));
        }
        if (value instanceof String) {
            String _string = (String) value;
            values = Arrays.asList((_string).split(","));
        }
        return values;
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
    public Object format(Object value) {
        if (elements == null) {
            return String.valueOf(value);
        }
        if (multi) {
            List<String> _format = toArray(value)
                    .stream()
                    .map(this::format0)
                    .collect(Collectors.toList());
            if (value instanceof String) {
                return String.join(",", _format);
            }
            return _format;
        }
        return format0(value);
    }

    private String format0(Object value) {
        return elements
            .stream()
            .filter(ele -> String.valueOf(value).equals(ele.value))
            .findFirst()
            .map(Element::getText)
            .orElse(String.valueOf(value));
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
