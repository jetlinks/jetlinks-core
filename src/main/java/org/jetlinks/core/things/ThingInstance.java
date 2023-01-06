package org.jetlinks.core.things;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
public class ThingInstance implements Serializable {
    private static final long serialVersionUID = -6849794470754667710L;

    /**
     * 物实例ID
     */
    @Nonnull
    private ThingId id;

    private String name;

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

    /**
     * 配置数据,可通过{@link Thing#getSelfConfig(String)}获取
     */
    private Map<String, Object> configuration;

    /**
     * 其他拓展数据,由不通的平台或者功能所需而决定,
     * 如: 该物实例所属维度信息等
     */
    private Map<String, Object> expands;
}
