package org.jetlinks.core.utils;

import io.netty.util.concurrent.FastThreadLocal;
import reactor.function.Consumer3;

import java.util.function.BiConsumer;

public final class StringBuilderUtils {

    private static final FastThreadLocal<StringBuilder> cacheBuilder = new FastThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder();
        }
    };

    public static <T, T2> String buildString(T data, T2 data2, Consumer3<T, T2, StringBuilder> builderFunction) {
        StringBuilder builder = cacheBuilder.get();
        builder.setLength(0);
        builderFunction.accept(data, data2, builder);
        return builder.toString();
    }

    public static <T> String buildString(T data, BiConsumer<T, StringBuilder> builderFunction) {
        return buildString(data, data, (t, t2, stringBuilder) -> builderFunction.accept(t, stringBuilder));
    }


}
