package org.jetlinks.core.things;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Map;

/**
 * 和物实例关联的其他业务数据
 */
@Getter
@Setter
public class ThingAssociatedData implements Serializable {
    private static final long serialVersionUID = -6849794470754667710L;

    /**
     * {@link ThingId#getType()}为数据类型,{@link ThingId#getId()}为数据唯一标识
     */
    @Nonnull
    private ThingId id;

    /**
     * 关联的物实例ID
     */
    @Nonnull
    private ThingId thingId;

    /**
     * 创建时间,UTC毫秒时间戳
     */
    private long createTime;

    /**
     * 更新时间,UTC毫秒时间戳
     */
    private long modifyTime;

    /**
     * 拓展字段数据,key为列名,value为值
     */
    private Map<String, Object> properties;

}
