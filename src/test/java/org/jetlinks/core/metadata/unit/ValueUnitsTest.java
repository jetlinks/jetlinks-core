package org.jetlinks.core.metadata.unit;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

public class ValueUnitsTest {
    @Test
    public void test(){
        Assert.assertNotNull(ValueUnits.lookup("meter"));

        Assert.assertFalse(ValueUnits.getAllUnit().isEmpty());
    }

    @Test
    public void testError(){

        ValueUnits.register(new ValueUnitSupplier() {
            @Override
            public Optional<ValueUnit> getById(String id) {
                throw new NoClassDefFoundError();
            }

            @Override
            public List<ValueUnit> getAll() {
                throw new NoClassDefFoundError();
            }
        });

        Assert.assertFalse(ValueUnits.getAllUnit().isEmpty());
        Assert.assertNotNull(ValueUnits.lookup("test_none"));

    }
}