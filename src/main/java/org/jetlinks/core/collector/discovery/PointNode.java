package org.jetlinks.core.collector.discovery;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.collector.AccessMode;

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

    private AccessMode[] accessModes;

    private Type nodeType;


    private String parentAddress;

    private Map<String, Object> others;

    public enum Type {
        point,
        directory
    }
}
