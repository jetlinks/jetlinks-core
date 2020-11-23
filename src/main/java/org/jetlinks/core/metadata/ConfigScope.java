package org.jetlinks.core.metadata;

/**
 * 配置作用域,请使用枚举实现此接口
 *
 * @author zhouhao
 * @see DeviceConfigScope
 * @since 1.1.4
 */
public interface ConfigScope {

    String getId();

    default String getName() {
        return getId();
    }

    static ConfigScope of(String id) {
        return () -> id;
    }
}
