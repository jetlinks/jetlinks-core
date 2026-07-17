package org.jetlinks.core.metadata;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.ProtocolSupport;

import java.util.Map;

/**
 * 固件包中解析出的元数据。
 * <p>
 * {@link #version} 是固件包内真实版本的投影，成功解析时必须非空且非空白；
 * {@link #metadata} 用于承载协议私有元数据，可以为空。
 * 本对象只描述解析结果，不负责元数据持久化，也不负责固件升级判断。
 *
 * @since 1.3.2
 * @see ProtocolSupport#parseFirmwareMetadata(FirmwareMetadataContext)
 */
@Getter
@Setter
public class FirmwareMetadata {

    /**
     * 固件包内的真实版本，成功解析时必填。
     */
    private String version;

    /**
     * 协议私有元数据，可为空。
     */
    private Map<String, Object> metadata;
}
