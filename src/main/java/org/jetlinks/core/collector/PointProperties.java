package org.jetlinks.core.collector;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class PointProperties {

    private String id;

    private AccessMode[] accessModes;

    private Map<String, Object> configuration;

}
