package org.jetlinks.core.metadata.unit;

import lombok.AllArgsConstructor;

@AllArgsConstructor(staticName = "of")
public class SymbolValueUnit implements ValueUnit {

    private final String symbol;

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public Object format(Object value) {
        if (value == null) {
            return null;
        }
        return value + "" + symbol;
    }

    @Override
    public String getId() {
        return "custom_" + symbol;
    }

    @Override
    public String getName() {
        return symbol;
    }

    @Override
    public String getDescription() {
        return symbol;
    }
}
