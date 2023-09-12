package org.jetlinks.core.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.config.ConfigKey;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SimpleFunctionMetadata implements FunctionMetadata {

    private String id;
    private String name;
    private String description;
    private Map<String, Object> expands;
    private List<PropertyMetadata> inputs;
    private DataType output;
    private boolean async;

    public static SimpleFunctionMetadata of(String id, String name, List<PropertyMetadata> inputs, DataType output) {
        return SimpleFunctionMetadata.of(id, name, null, null, inputs, output, false);
    }

    public <T> SimpleFunctionMetadata expand(ConfigKey<T> key, T value) {
        return expand(key.getKey(), value);
    }

    public synchronized SimpleFunctionMetadata expand(String key, Object value) {
        if (expands == null) {
            expands = new HashMap<>();
        }
        expands.put(key, value);
        return this;
    }

    @Override
    public @NotNull List<PropertyMetadata> getInputs() {
        if (inputs == null) {
            return Collections.emptyList();
        }
        return inputs;
    }

    @Override
    public FunctionMetadata merge(FunctionMetadata another, MergeOption... option) {
        return another;
    }
}
