package org.jetlinks.core.utils;

import reactor.function.*;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class IterableUtils {

    public static <V, T> V itr(Iterable<T> source,
                               Function<T, V> handler,
                               BiFunction<V, V, V> reducer) {
        return itr(source, null, null, null, null, handler,
                   ((t, a0, a1, nil0, nil, callback) -> callback.apply(t)),
                   reducer);
    }

    public static <A0, V, T> V itr(Iterable<T> source,
                                   A0 arg0,
                                   BiFunction<T, A0, V> handler,
                                   BiFunction<V, V, V> reducer) {
        return itr(source, arg0, null, null, null, handler,
                   ((t, a0, a1, nil0, nil, callback) ->
                          callback.apply(t, a0)),
                   reducer);
    }


    public static <A0, A1, V, T> V itr(Iterable<T> source,
                                       A0 arg0, A1 arg1,
                                       Function3<T, A0, A1, V> handler,
                                       BiFunction<V, V, V> reducer) {
        return itr(source, arg0, arg1, null, null, handler,
                   ((t, a0, a1, nil0, nil, callback) ->
                          callback.apply(t, a0, a1)),
                   reducer);
    }


    public static <A0, A1, A2, V, T> V itr(Iterable<T> source,
                                           A0 arg0, A1 arg1, A2 arg2,
                                           Function4<T, A0, A1, A2, V> handler,
                                           BiFunction<V, V, V> reducer) {
        return itr(source, arg0, arg1, arg2, null, handler,
                   ((t, a0, a1, a2, nil, callback) ->
                          callback.apply(t, a0, a1, a2)),
                   reducer);
    }

    public static <A0, A1, A2, A3, V, T> V itr(Iterable<T> source,
                                               A0 arg0, A1 arg1, A2 arg2, A3 arg3,
                                               Function5<T, A0, A1, A2, A3, V> handler,
                                               BiFunction<V, V, V> reducer) {
        return itr(source, arg0, arg1, arg2, arg3, handler,
                   ((t, a0, a1, a2, a3, callback) ->
                          callback.apply(t, a0, a1, a2, a3)),
                   reducer);
    }

    public static <A0, A1, A2, A3, A4, V, T> V itr(
        Iterable<T> source,
        A0 arg0, A1 arg1, A2 arg2, A3 arg3, A4 arg4,
        Function6<T, A0, A1, A2, A3, A4, V> handler,
        BiFunction<V, V, V> reducer) {
        V v = null;
        for (T e : source) {
            V v_ = handler.apply(e, arg0, arg1, arg2, arg3, arg4);
            if (v == null) {
                v = v_;
            } else if (reducer != null) {
                v = reducer.apply(v, v_);
            }
        }
        return v;
    }

    public static <A0, A1, T> void itr(Iterable<T> source,
                                       A0 arg0, A1 arg1,
                                       Consumer3<T, A0, A1> handler) {
        itr(source, arg0, arg1, null, null, handler,
            ((t, a0, a1, nil0, nil, callback) -> callback.accept(t, a0, a1)));
    }

    public static <A0, T> void itr(Iterable<T> source,
                                   A0 arg0,
                                   BiConsumer<T, A0> handler) {
        itr(source, arg0, null, null, null, handler,
            ((t, a0, a1, nil0, nil, callback) -> callback.accept(t, a0)));
    }


    public static <T> void itr(Iterable<T> source,
                               Consumer<T> handler) {
        itr(source, null, null, null, null, handler,
            ((t, a0, a1, nil0, nil, callback) -> callback.accept(t)));
    }

    public static <A0, A1, A2, A3, A4, T> void itr(
        Iterable<T> source,
        A0 arg0, A1 arg1, A2 arg2, A3 arg3, A4 arg4,
        Consumer6<T, A0, A1, A2, A3, A4> consumer6) {
        for (T e : source) {
            consumer6.accept(e, arg0, arg1, arg2, arg3, arg4);
        }
    }

}
