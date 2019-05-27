package org.jetlinks.core.device;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class DeviceProductInfo implements Serializable {

    private String id;

    private String name;

    private String projectId;

    private String projectName;

    private String protocol;

}
