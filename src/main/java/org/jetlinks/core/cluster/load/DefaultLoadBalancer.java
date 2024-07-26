package org.jetlinks.core.cluster.load;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;

class DefaultLoadBalancer<S> implements LoadBalancer<S> {

    private final HashFunction hashFunction = Hashing.murmur3_128();

    private final NavigableMap<Long, S> circle;

    private final int vnode = 64;

    public DefaultLoadBalancer() {
        circle = new ConcurrentSkipListMap<>();
    }

    protected long hash(Object key) {
        long hashCode;
        if (key instanceof Number) {
            hashCode =
                hashFunction
                    .hashLong(((Number) key).longValue())
                    .asLong();
        } else {
            hashCode = hashFunction
                .hashString(String.valueOf(key), StandardCharsets.UTF_8)
                .asLong();
        }
        return hashCode;
    }

    @Override
    public void register(S server) {
        circle.put(hash(server), server);
        for (int i = 0; i < vnode; i++) {
            circle.put(hash(server + "&VN=" + i), server);
        }
    }

    @Override
    public void deregister(S server) {
        circle.remove(hash(server), server);
        for (int i = 0; i < vnode; i++) {
            circle.remove(hash(server + "&VN=" + i), server);
        }
    }

    @Override
    public S choose() {
        return choose(ThreadLocalRandom.current().nextLong());
    }

    @Override
    public S choose(Object key) {
        long hash = hash(key);
        if (circle.isEmpty()) {
            return null;
        }
        S fast = circle.get(hash);
        if (fast != null) {
            return fast;
        }
        Map.Entry<Long, S> entry = circle.tailMap(hash, true).firstEntry();
        if (entry == null) {
            entry = circle.firstEntry();
        }
        return entry.getValue();
    }
}
