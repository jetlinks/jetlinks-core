package org.jetlinks.core.metadata.unit;

import java.util.Optional;

public interface ValueUnitSupplier {

    Optional<ValueUnit> getById(String id);
}
