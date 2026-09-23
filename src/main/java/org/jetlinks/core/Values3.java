package org.jetlinks.core;

import java.util.Collections;
import java.util.Map;

final class Values3 extends Values2 {

    private final String thirdKey;
    private final Object thirdValue;

    Values3(String firstKey, Object firstValue, String secondKey, Object secondValue,
            String thirdKey, Object thirdValue) {
        super(firstKey, firstValue, secondKey, secondValue);
        this.thirdKey = thirdKey;
        this.thirdValue = thirdValue;
    }

    @Override
    protected Object getRaw(String key) {
        if (key != null && key.equals(thirdKey)) {
            return thirdValue;
        }
        return super.getRaw(key);
    }

    @Override
    public int size() {
        return super.size() + (thirdValue == null ? 0 : 1);
    }

    @Override
    public Map<String, Object> getAllValues() {
        if (thirdValue == null) {
            return super.getAllValues();
        }
        if (firstValue == null) {
            return secondValue == null
                ? Collections.singletonMap(thirdKey, thirdValue)
                : Map.of(secondKey, secondValue, thirdKey, thirdValue);
        }
        if (secondValue == null) {
            return Map.of(firstKey, firstValue, thirdKey, thirdValue);
        }
        return Map.of(firstKey, firstValue, secondKey, secondValue, thirdKey, thirdValue);
    }
}
