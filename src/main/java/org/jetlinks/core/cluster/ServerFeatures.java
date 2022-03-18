package org.jetlinks.core.cluster;

import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ServerFeatures {

    public static final Map<String, ServerFeature> features = new ConcurrentHashMap<>();

    public static Disposable addFeature(ServerFeature feature) {
        features.put(feature.getId(), feature);
        return () -> features.remove(feature.getId(), feature);
    }

    public static List<ServerFeature> features() {
        return new ArrayList<>(features.values());
    }

    public static Optional<ServerFeature> feature(String id) {
        return Optional.ofNullable(features.get(id));
    }

}
