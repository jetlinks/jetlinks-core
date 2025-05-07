package org.jetlinks.core.utils;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class ReactorHooks {

    static final List<Function<Mono<Object>, Mono<Object>>> HOOKS = new CopyOnWriteArrayList<>();


    @SuppressWarnings("all")
    static <T> Mono<T> doHook(Mono<T> mono) {
        for (Function<Mono<Object>, Mono<Object>> hook : HOOKS) {
            mono = hook.apply((Mono) mono);
        }
        return (Mono<T>) mono;
    }

    /**
     * 自定义阻塞获取Mono结果时的钩子函数.
     *
     * @param operator 钩子函数
     */
    public static void hookBlocking(Function<Mono<Object>, Mono<Object>> operator) {
        HOOKS.add(operator);
    }


}
