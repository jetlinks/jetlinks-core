package org.jetlinks.core.collector.discovery;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.collector.AccessMode;
import org.jetlinks.core.metadata.DataType;

import java.util.Map;

@Getter
@Setter
public class PointNode {

    /**
     * 点位地址
     */
    private String address;

    /**
     * 点位名称
     */
    private String name;

    /**
     * 支持的访问模式
     */
    private AccessMode[] accessModes;

    /**
     * 点位类型
     */
    private Type nodeType;

    /**
     * 上级地址
     */
    private String parentAddress;

    /**
     * 数据类型
     */
    private DataType dataType;

    /**
     * 其他配置信息
     */
    private Map<String, Object> others;

    public enum Type {
        // 点位
        point,
        // 目录
        directory
    }
}
