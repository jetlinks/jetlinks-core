package org.jetlinks.core.metadata.types;

import org.jetlinks.core.metadata.DataType;

public interface DateTimeType extends DataType {

    @Override
    default String getId() {
        return "date";
    }

    @Override
    default String getName() {
        return "时间";
    }

    @Override
    default String getDescription() {
        return "时间";
    }

    String getFormat();

    boolean isTimestamp();

}
