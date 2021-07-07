package org.jetlinks.core.utils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

/**
 * 并行递增工具类,通常用于计算并行操作的延迟。
 * <p>
 * 比如在发送设备消息时,设备不支持并行,需要将消息串行执行,
 * 则可以使用此工具计算下一次发送的延迟,然后利用{@link reactor.core.publisher.Mono#delay(Duration)}来延迟发送.
 * <p>
 * <pre>
 *  private ParallelIntervalHelper intervalHelper = ParallelIntervalHelper.create(Duration.ofSeconds(1));
 *
 *
 *     public Mono&lt;EncodedMessage&gt; encode(MessageDecodeContext context){
 *         //根据设备ID作为key进行计算
 *         long delay = intervalHelper.next(context.getDevice().getDeviceId());
 *
 *         EncodedMessage msg = doEncode(context);
 *
 *         return delay > 0
 *         ? Mono.delay(Duration.ofMillis(delay)).thenReturn(msg)
 *         : Mono.just(msg)
 *     }
 *
 * </pre>
 *
 * @author zhouhao
 * @since 1.1.7
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParallelIntervalHelper {

    private final long interval;

    private final ConcurrentMap<String, Info> times = new ConcurrentHashMap<>();

    public static ParallelIntervalHelper create(Duration interval) {
        return new ParallelIntervalHelper(interval.toMillis());
    }

    /**
     * 获取下一次的间隔(毫秒),如果上一次与本次获取的时间超过间隔则返回0
     *
     * @param key key
     * @return 间隔(毫秒)
     */
    public long next(@Nonnull String key) {
        Info last = times.compute(key, (k, old) -> {
            long now = System.currentTimeMillis();
            if (old == null) {
                //第一次
                return new Info(0, now);
            }
            old.next(now, interval);
            return old;
        });

        return last.interval;
    }

    /**
     * 获取当前的间隔(毫秒),如果上一次与本次获取的时间超过间隔则返回0
     *
     * @param key key
     * @return 间隔(毫秒)
     */
    public long current(@Nonnull String key) {
        Info info = times.get(key);
        if (info == null) {
            return 0;
        }
        return info.current(System.currentTimeMillis(), interval);
    }

    /**
     * 尝试对{@link Flux}进行延迟操作
     *
     * @param key    key
     * @param source Flux
     * @param <T>    Flux泛型类型
     * @return 延迟后的FLux
     */
    public <T> Flux<T> delay(@Nonnull String key, @Nonnull Flux<T> source) {
        return source
                .flatMap(e -> delay(key, Mono.just(e), Mono::delayElement));
    }

    /**
     * 尝试对{@link Mono}进行延迟操作
     *
     * @param key    key
     * @param source Mono
     * @param <T>    Mono泛型类型
     * @return 延迟后的Mono
     */
    public <T> Mono<T> delay(@Nonnull String key, @Nonnull Mono<T> source) {
        return delay(key, source, Mono::delayElement);
    }

    /**
     * 根据key返回延迟{@link Mono}
     *
     * @param key key
     * @return 延迟Mono
     */
    public Mono<Void> delay(@Nonnull String key) {
        return this
                .delay(key, Mono.just(1), Mono::delayElement)
                .then();
    }

    /**
     * 根据key和指定的数据进行延迟转换,如果不存在延迟则直接返回源数据
     *
     * @param key    key
     * @param source 源数据
     * @param mapper 转换器
     * @param <S>    数据类型
     * @return 转换后的类型
     */
    public <S> S delay(@Nonnull String key, S source, BiFunction<S, Duration, S> mapper) {
        long delay = next(key);
        if (delay > 0) {
            return mapper.apply(source, Duration.ofMillis(delay));
        }
        return source;
    }

    @AllArgsConstructor
    private static class Info {
        private long interval;
        private long lastTime;

        public long current(long now, long interval) {
            if (now - lastTime > interval) {
                return 0;
            }
            return this.interval;
        }

        public synchronized void next(long now, long interval) {
            long requestInterval = now - lastTime;

            //递增
            this.interval = Math.max(0, (this.interval - requestInterval) + interval);
            this.lastTime = now;
        }
    }
}
