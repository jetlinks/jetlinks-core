package org.jetlinks.core.metadata.unit;


import java.util.List;
import java.util.Optional;

public interface ValueUnitSupplier {

    Optional<ValueUnit> getById(String id);

    List<ValueUnit> getAll();
}
