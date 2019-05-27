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
public class DeviceInfo implements Serializable {
    private static final long serialVersionUID = -6849794470754667710L;

    /**
     * 设备ID
     */
    private String id;

    /**
     * 产品-型号ID
     */
    private String productId;

    /**
     * 产品-型号名称
     */
    private String productName;

    /**
     * 设备类型
     */
    private byte type;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 创建人
     */
    private String creatorId;

    /**
     * 创建人名称
     */
    private String creatorName;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 上级设备ID
     */
    private String parentDeviceId;

    /**
     * 消息协议
     */
    private String protocol;
}
