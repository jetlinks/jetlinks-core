package org.jetlinks.core.metadata;


/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface EventMetadata extends Metadata ,Jsonable{

    DataType getType();

    default EventMetadata merge(EventMetadata another, MergeOption... option){
        throw new UnsupportedOperationException("不支持事件物模型合并");
    }
}
