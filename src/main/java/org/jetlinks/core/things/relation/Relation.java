package org.jetlinks.core.things.relation;

import java.util.Collections;
import java.util.Map;

public interface Relation {
    String getId();

    String getName();

    /**
     * 反转关系名称
     */
    default String getReverseName() {
        return null;
    }

    /**
     * 是否为反转关系
     */
    default boolean isReverse() {
        return false;
    }

    default Map<String, Object> getExpands() {
        return Collections.emptyMap();
    }
}
