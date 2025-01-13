package org.jetlinks.core.event;

import lombok.AllArgsConstructor;
import org.jetlinks.core.Routable;

import java.util.function.Supplier;

@AllArgsConstructor(staticName = "of")
public class RoutableSupplier<T> implements Supplier<T>, Routable {

    private final Object routeKey;

    private final Supplier<T> supplier;

    @Override
    public T get() {
        return supplier.get();
    }

    @Override
    public Object routeKey() {
        return routeKey;
    }
}
