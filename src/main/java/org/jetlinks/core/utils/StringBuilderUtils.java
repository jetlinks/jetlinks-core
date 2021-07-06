package org.jetlinks.core.utils;

import io.netty.util.concurrent.FastThreadLocal;

import java.util.function.BiConsumer;

public final class StringBuilderUtils {

    private static final FastThreadLocal<StringBuilder> cacheBuilder = new FastThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder();
        }
    };

    public static <T> String buildString(T data, BiConsumer<T, StringBuilder> builderFunction) {
        StringBuilder builder = cacheBuilder.get();
        builder.setLength(0);
        builderFunction.accept(data, builder);
        return builder.toString();
    }


}
