package org.jetlinks.core.utils;

import org.hswebframework.web.recycler.Recycler;
import reactor.function.Consumer3;
import reactor.function.Consumer4;
import reactor.function.Consumer5;

import java.util.function.BiConsumer;

public final class StringBuilderUtils {

    public static final Recycler<StringBuilder> sharedBuilder = Recycler.stringBuilder();


    public static <T, T2, T3, T4> String buildString(T data, T2 data2, T3 data3, T4 data4, Consumer5<T, T2, T3, T4, StringBuilder> builderFunction) {

        return sharedBuilder.doWith(
            builderFunction, data, data2, data3, data4,
            (sb, fn, _1, _2, _3, _4) -> {
                fn.accept(_1, _2, _3, _4, sb);
                return sb.toString();
            });
    }

    public static <T, T2, T3> String buildString(T data, T2 data2, T3 data3, Consumer4<T, T2, T3, StringBuilder> builderFunction) {
        return sharedBuilder.doWith(
            builderFunction, data, data2, data3,
            (sb, fn, _1, _2, _3) -> {
                fn.accept(_1, _2, _3, sb);
                return sb.toString();
            });
    }

    public static <T, T2> String buildString(T data, T2 data2, Consumer3<T, T2, StringBuilder> builderFunction) {
        return sharedBuilder.doWith(
            builderFunction, data, data2,
            (sb, fn, _1, _2) -> {
                fn.accept(_1, _2, sb);
                return sb.toString();
            });
    }

    public static <T> String buildString(T data, BiConsumer<T, StringBuilder> builderFunction) {
        return sharedBuilder.doWith(
            builderFunction, data,
            (sb, fn, _1) -> {
                fn.accept(_1, sb);
                return sb.toString();
            });
    }


}
