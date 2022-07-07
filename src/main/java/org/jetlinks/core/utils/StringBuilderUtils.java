package org.jetlinks.core.utils;

import io.netty.util.concurrent.FastThreadLocal;
import reactor.function.Consumer3;
import reactor.function.Consumer4;
import reactor.function.Consumer5;

import java.util.function.BiConsumer;

public final class StringBuilderUtils {

    private static final FastThreadLocal<StringBuilder> cacheBuilder = new FastThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder();
        }
    };

    private static StringBuilder takeBuilder() {
        return cacheBuilder.get();
    }

    public static <T, T2, T3, T4> String buildString(T data, T2 data2, T3 data3, T4 data4, Consumer5<T, T2, T3, T4, StringBuilder> builderFunction) {
        StringBuilder builder = takeBuilder();
        try {
            builderFunction.accept(data, data2, data3, data4, builder);
            return builder.toString();
        } finally {
            builder.setLength(0);
        }
    }

    public static <T, T2, T3> String buildString(T data, T2 data2, T3 data3, Consumer4<T, T2, T3, StringBuilder> builderFunction) {
        StringBuilder builder = takeBuilder();
        try {
            builderFunction.accept(data, data2, data3, builder);
            return builder.toString();
        } finally {
            builder.setLength(0);
        }
    }

    public static <T, T2> String buildString(T data, T2 data2, Consumer3<T, T2, StringBuilder> builderFunction) {
        StringBuilder builder = takeBuilder();
        try {
            builderFunction.accept(data, data2, builder);
            return builder.toString();
        } finally {
            builder.setLength(0);
        }
    }

    public static <T> String buildString(T data, BiConsumer<T, StringBuilder> builderFunction) {
        StringBuilder builder = takeBuilder();
        try {
            builderFunction.accept(data, builder);
            return builder.toString();
        } finally {
            builder.setLength(0);
        }
    }


}
