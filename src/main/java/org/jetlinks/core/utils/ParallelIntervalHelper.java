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
     * 获取下一次的间隔(毫秒)
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
            //距离上一次访问已经超过间隔
            if (now - old.createTime > interval) {
                old.increment = 0;
            } else {
                //延迟递增
                old.increment = old.increment + interval;
            }
            old.createTime = now;
            return old;
        });

        return last.increment;
    }

    @AllArgsConstructor
    private static class Info {
        private long increment;
        private long createTime;
    }
}
