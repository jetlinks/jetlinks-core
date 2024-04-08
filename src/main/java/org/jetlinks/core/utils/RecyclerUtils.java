package org.jetlinks.core.utils;

import io.netty.util.Recycler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public class RecyclerUtils {

    private static final Map<Object, Object> sharedObjects = new ConcurrentReferenceHashMap<>(
        65535,
        ConcurrentReferenceHashMap.ReferenceType.SOFT);

    @SuppressWarnings("all")
    public static <T> T intern(T str) {
        return str == null ? null : (T) sharedObjects.computeIfAbsent(str, Function.identity());
    }

    public static <T> Recycler<T> newRecycler(Class<T> type, Function<Recycler.Handle<T>, T> objectSupplier, int defaultRatio) {
        int maxCapacityPerThread = getPoolConfig(type, "maxCapacityPerThread")
            .map(Integer::parseInt)
            .orElse(4096);

        int maxSharedCapacityFactor = getPoolConfig(type, "maxSharedCapacityFactor")
            .map(Integer::parseInt)
            .orElse(2);

        int maxDelayedQueuesPerThread = getPoolConfig(type, "maxDelayedQueuesPerThread")
            .map(Integer::parseInt)
            .orElse(Runtime.getRuntime().availableProcessors() * 2);

        int ratio = getPoolConfig(type, "ratio")
            .map(Integer::parseInt)
            .orElse(defaultRatio);

        if (log.isDebugEnabled()) {
            log.debug("-D{}: {}", getConfigName(type, "maxCapacityPerThread"), maxCapacityPerThread);
            log.debug("-D{}: {}", getConfigName(type, "maxSharedCapacityFactor"), maxSharedCapacityFactor);
            log.debug("-D{}: {}", getConfigName(type, "maxDelayedQueuesPerThread"), maxDelayedQueuesPerThread);
            log.debug("-D{}: {}", getConfigName(type, "ratio"), ratio);
        }
        return new Recycler<T>(maxCapacityPerThread, maxSharedCapacityFactor, ratio, maxDelayedQueuesPerThread) {
            @Override
            protected T newObject(Handle<T> handle) {
                return objectSupplier.apply(handle);
            }
        };
    }

    public static <T> Recycler<T> newRecycler(Class<T> type, Function<Recycler.Handle<T>, T> objectSupplier) {
        return newRecycler(type, objectSupplier, 8);

    }

    private static Optional<String> getPoolConfig(Class<?> type, String key) {
        return Optional.ofNullable(System.getProperty(getConfigName(type, key)));
    }

    private static String getConfigName(Class<?> type, String key) {
        return (type.getName() + ".pool." + key).replace("$$", ".").replace("$", ".");
    }

    public static <T> RecyclableDequeue<T> dequeue() {
        return RecyclableDequeue.newInstance();
    }
}
