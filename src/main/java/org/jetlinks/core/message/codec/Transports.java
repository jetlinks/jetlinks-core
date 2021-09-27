package org.jetlinks.core.message.codec;

import reactor.core.Disposable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Transports {

    private static final Map<String, Transport> all = new ConcurrentHashMap<>();

    public static Disposable register(Collection<Transport> transport) {
        transport.forEach(Transports::register);
        return () -> {
            for (Transport t : transport) {
                all.remove(t.getId().toLowerCase(Locale.ROOT));
            }
        };
    }

    public static Disposable register(Transport transport) {
        all.put(transport.getId().toUpperCase(), transport);
        return () -> all.remove(transport.getId().toUpperCase());
    }

    public static List<Transport> get() {
        return new ArrayList<>(all.values());
    }

    public static Optional<Transport> lookup(String id) {
        return Optional.ofNullable(all.get(id.toUpperCase()));
    }

}
