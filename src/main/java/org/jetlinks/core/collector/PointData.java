package org.jetlinks.core.collector;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PointData {

    private String id;

    private String state;

    private byte[] nativeData;

    private Object data;

    private long timestamp;


}
