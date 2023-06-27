package org.jetlinks.core.trace;

import io.opentelemetry.context.propagation.TextMapGetter;

import javax.annotation.Nullable;
import java.util.Map;

public class HeaderMapTextMapGetter implements TextMapGetter<Map<String, ?>> {

    private static final HeaderMapTextMapGetter INSTANCE = new HeaderMapTextMapGetter();

    public static TextMapGetter<Map<String, ?>> instance() {
        return INSTANCE;
    }

    private HeaderMapTextMapGetter() {

    }

    @Override
    public Iterable<String> keys(Map<String, ?> carrier) {
        return carrier.keySet();
    }

    @Nullable
    @Override
    public String get(@Nullable Map<String, ?> carrier, String key) {
        if (carrier == null) {
            return null;
        }

        String value = (String) carrier.get(key);

        if (value == null) {
            Object headers = carrier.get("headers");
            if (headers instanceof Map) {
                value = (String) ((Map<?, ?>) headers).get(key);
            }
        }

        return value;
    }
}
