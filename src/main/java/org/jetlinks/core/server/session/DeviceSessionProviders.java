package org.jetlinks.core.server.session;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceSessionProviders {

    public static final Map<String, DeviceSessionProvider> providers = new ConcurrentHashMap<>();

    static {
        KeepOnlineDeviceSessionProvider.load();
    }

    public static void register(DeviceSessionProvider provider) {
        providers.put(provider.getId(), provider);
    }

    public static Optional<DeviceSessionProvider> lookup(String id) {
        return Optional.ofNullable(providers.get(id));
    }
}
