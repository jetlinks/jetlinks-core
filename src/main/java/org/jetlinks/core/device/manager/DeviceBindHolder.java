package org.jetlinks.core.device.manager;

import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DeviceBindHolder {

    private static final Map<String, DeviceBindProvider> suppliers = new ConcurrentHashMap<>();

    public static void addSupplier(DeviceBindProvider supplier) {
        DeviceBindProvider old = suppliers.put(supplier.getId(), supplier);
        if (old instanceof Disposable) {
            ((Disposable) old).dispose();
        }
    }

    public static Optional<DeviceBindProvider> lookup(String id) {
        return Optional.ofNullable(suppliers.get(id));
    }

    public static List<DeviceBindProvider> getAllProvider(){
        return new ArrayList<>(suppliers.values());
    }

}
