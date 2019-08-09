package org.jetlinks.core.metadata.types;


import org.jetlinks.core.metadata.DataType;

import java.io.Serializable;
import java.util.List;

public interface EnumType extends DataType {

    @Override
    default String getId() {
        return "enum";
    }

    @Override
    default String getName() {
        return "枚举";
    }

    @Override
    default String getDescription() {
        return "枚举类型";
    }

    List<Element> getElements();


    class Element implements Serializable {
        private String value;

        private String text;
    }
}
