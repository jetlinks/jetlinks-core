package org.jetlinks.core.message.codec.context;

import lombok.AllArgsConstructor;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.RepayableDeviceMessage;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class CacheCodecContext implements CodecContext {

    private final Map<Object, Cache> caches = new ConcurrentHashMap<>();

    void checkExpires() {
        for (Map.Entry<Object, Cache> entry : caches.entrySet()) {
            if (entry.getValue().isExpired()) {
                caches.remove(entry.getKey());
            }
        }
    }

    @Override
    public void cacheDownstream(Object key, RepayableDeviceMessage<? extends DeviceMessageReply> message, Duration ttl) {
        caches.put(key, new Cache(System.currentTimeMillis(), ttl.toMillis(), message));
        if (caches.size() > 100) {
            checkExpires();
        }
    }

    @Override
    public <T extends RepayableDeviceMessage<? extends DeviceMessageReply>> Optional<T> getDownstream(Object key, boolean remove) {
        return Optional
                .ofNullable(remove ? caches.remove(key) : caches.get(key))
                .map(cache -> {
                    if (cache.isAlive()) {
                        return cache.getMessage();
                    }
                    caches.remove(key, cache);
                    return null;
                });
    }


    @AllArgsConstructor
    static class Cache {
        long cacheTime;
        long ttl;
        RepayableDeviceMessage<? extends DeviceMessageReply> msg;

        boolean isAlive() {
            return !isExpired();
        }

        boolean isExpired() {
            return ttl > 0 && System.currentTimeMillis() - cacheTime >= ttl;
        }

        @SuppressWarnings("all")
        <T extends RepayableDeviceMessage<? extends DeviceMessageReply>> T getMessage() {
            return (T) msg;
        }
    }
}
