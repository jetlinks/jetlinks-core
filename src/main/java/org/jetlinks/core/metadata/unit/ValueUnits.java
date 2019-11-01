package org.jetlinks.core.metadata.unit;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class ValueUnits {

    private static final List<ValueUnitSupplier> suppliers = new CopyOnWriteArrayList<>();

    static {
        ValueUnits.register((id) -> Optional.ofNullable(UnifyUnit.of(id)));
    }

    public static void register(ValueUnitSupplier supplier) {
        suppliers.add(supplier);
    }

    public static Optional<ValueUnit> lookup(String id) {
        for (ValueUnitSupplier supplier : suppliers) {
            Optional<ValueUnit> unit = supplier.getById(id);
            if (unit.isPresent()) {
                return unit;
            }
        }
        return Optional.empty();
    }
}
