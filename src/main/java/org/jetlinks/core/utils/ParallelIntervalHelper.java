package org.jetlinks.core.utils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

            //距离上一次访问已经超过间隔
            //  if (this.interval > 0 && requestInterval > this.interval) {
            //   this.interval = 0;
            //  } else {
            //递增
            this.interval = Math.max(0, (this.interval - requestInterval) + interval);
            // }
            this.lastTime = now;
        }
    }
}
