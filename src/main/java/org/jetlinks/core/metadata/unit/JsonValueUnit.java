package org.jetlinks.core.metadata.unit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;

import javax.annotation.Nullable;

@AllArgsConstructor
public class JsonValueUnit implements ValueUnit {

    private final String id;

    private final String symbol;

    private final String name;

    public JsonValueUnit(String symbol, String name) {
        this(symbol, symbol, name);
    }

    @Nullable
    public static JsonValueUnit of(String jsonStr) {

        JSONObject json = JSON.parseObject(jsonStr);

        String symbol = json.getString("symbol");
        if (null == symbol) {
            return null;
        }

        return new JsonValueUnit(String.valueOf(json.getOrDefault("id", symbol)),
                                 symbol,
                                 (String) json.getOrDefault("name", symbol));
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public Object format(Object value) {
        if (value == null) {
            return null;
        }
        return value + symbol;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return symbol;
    }
}
