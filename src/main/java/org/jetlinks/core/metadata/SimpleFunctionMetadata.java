package org.jetlinks.core.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.Collections;
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
