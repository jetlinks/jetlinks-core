package org.jetlinks.core.metadata;


import java.util.List;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface EventMetadata extends Metadata ,Jsonable{

    List<PropertyMetadata> getParameters();

}
