package org.jetlinks.core;

/**
 * 功能模块接口,用于定义个功能模块
 *
 * @author zhouhao
 * @see EnumModule
 * @since 1.3.2
 */
public interface Module {

    /**
     * ID 标识
     *
     * @return 标识
     */
    String getId();

    /**
     * 名称
     *
     * @return 名称
     */
    String getName();

    /**
     * @return 描述
     */
    String getDescription();
}
