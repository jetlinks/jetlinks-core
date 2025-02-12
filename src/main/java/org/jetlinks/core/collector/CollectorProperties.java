package org.jetlinks.core.collector;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CollectorProperties {

    private String id;

    private Map<String,Object> configuration;

}
