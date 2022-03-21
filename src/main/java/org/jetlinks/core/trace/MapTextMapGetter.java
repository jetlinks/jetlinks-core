package org.jetlinks.core.trace;

import io.opentelemetry.context.propagation.TextMapGetter;

import javax.annotation.Nullable;
import java.util.Map;

public class MapTextMapGetter implements TextMapGetter<Map<String, ?>> {

    private static final MapTextMapGetter INSTANCE = new MapTextMapGetter();

    public static TextMapGetter<Map<String, ?>> instance() {
        return INSTANCE;
    }

    private MapTextMapGetter() {

    }

    @Override
    public Iterable<String> keys(Map<String, ?> carrier) {
        return carrier.keySet();
    }

    @Nullable
    @Override
    public String get(@Nullable Map<String, ?> carrier, String key) {
        return carrier == null ? null : (String) carrier.get(key);
    }
}
