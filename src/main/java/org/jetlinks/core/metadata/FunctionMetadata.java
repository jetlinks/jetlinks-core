package org.jetlinks.core.metadata;

import java.util.List;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface FunctionMetadata extends Metadata ,Jsonable{

    List<PropertyMetadata> getInputs();

    PropertyMetadata getOutput();

    boolean isAsync();

}
