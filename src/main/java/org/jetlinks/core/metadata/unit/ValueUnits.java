package org.jetlinks.core.metadata.unit;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ValueUnits {

    private static final List<ValueUnitSupplier> suppliers = new CopyOnWriteArrayList<>();

    static {
        ValueUnits.register(new ValueUnitSupplier() {
            @Override
            public Optional<ValueUnit> getById(String id) {
                return Optional.ofNullable(UnifyUnit.of(id));
            }

            @Override
            public List<ValueUnit> getAll() {
                return Arrays.asList(UnifyUnit.values());
            }
        });
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
        //json ?
        if (id.startsWith("{")) {
            return Optional.ofNullable(JsonValueUnit.of(id));
        }
        return Optional.of(SymbolValueUnit.of(id));
    }

    public static List<ValueUnit> getAllUnit() {
        return suppliers.stream()
                .map(ValueUnitSupplier::getAll)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
