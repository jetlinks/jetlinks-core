package org.jetlinks.core.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.cache.CacheBuilder;
import org.jctools.maps.NonBlockingHashMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public class Caches {

    private static final Supplier<ConcurrentMap<Object, Object>> cacheSupplier;

    private static boolean caffeinePresent() {
        if (Boolean.getBoolean("jetlinks.cache.caffeine.disabled")) {
            return false;
        }
        try {
            Class.forName("com.github.benmanes.caffeine.cache.Cache");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean jctoolPresent() {
        if (Boolean.getBoolean("jetlinks.cache.jctool.disabled")) {
            return false;
        }
        try {
            Class.forName("org.jctools.maps.NonBlockingHashMap");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean guavaPresent() {
        return !Boolean.getBoolean("jetlinks.cache.guava.disabled");
    }

    private static ConcurrentMap<Object, Object> createCaffeine() {
        return Caffeine.newBuilder().build().asMap();
    }

    private static ConcurrentMap<Object, Object> createGuava() {
        return CacheBuilder.newBuilder().build().asMap();
    }


    static {
        if (jctoolPresent()) {
            cacheSupplier = NonBlockingHashMap::new;
        } else if (caffeinePresent()) {
            cacheSupplier = Caches::createCaffeine;
        } else if (guavaPresent()) {
            cacheSupplier = Caches::createGuava;
        } else {
            cacheSupplier = ConcurrentHashMap::new;
        }
    }

    @SuppressWarnings("all")
    public static <K, V> ConcurrentMap<K, V> newCache() {
        return (ConcurrentMap) cacheSupplier.get();
    }

}
