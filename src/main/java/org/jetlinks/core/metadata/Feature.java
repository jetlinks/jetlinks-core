package org.jetlinks.core.metadata;

/**
 * 特性接口,一般使用枚举实现。用于定义协议或者设备支持的某些特性.
 *
 * @author zhouhao
 * @see MetadataFeature
 * @see ManagementFeature
 * @see SimpleFeature
 * @since 1.1.6
 */
public interface Feature {
    /**
     * @return 唯一标识
     */
    String getId();

    /**
     * @return 名称
     */
    String getName();

    static Feature of(String id, String name) {
        return new SimpleFeature(id, name);
    }

}
