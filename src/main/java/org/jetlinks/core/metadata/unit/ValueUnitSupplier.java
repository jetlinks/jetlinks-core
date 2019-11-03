package org.jetlinks.core.metadata.unit;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

public interface ValueUnitSupplier {

    Optional<ValueUnit> getById(String id);

    List<ValueUnit> getAll();
}
