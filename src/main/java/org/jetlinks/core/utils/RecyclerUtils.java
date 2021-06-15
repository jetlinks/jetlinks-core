package org.jetlinks.core.utils;

import io.netty.util.Recycler;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
public class RecyclerUtils {

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

        if(log.isDebugEnabled()){
            log.debug("-D{}.pool.maxCapacityPerThread: {}",type.getName(),maxCapacityPerThread);
            log.debug("-D{}.pool.maxSharedCapacityFactor: {}",type.getName(),maxSharedCapacityFactor);
            log.debug("-D{}.pool.maxDelayedQueuesPerThread: {}",type.getName(),maxDelayedQueuesPerThread);
            log.debug("-D{}.pool.ratio: {}",type.getName(),ratio);
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
        return Optional.ofNullable(System.getProperty(type.getName() + ".pool." + key));
    }
}
