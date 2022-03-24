package org.jetlinks.core.spi;

import org.jetlinks.core.ProtocolSupport;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * 设备协议支持提供商
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface ProtocolSupportProvider extends Disposable {

    /**
     * 创建协议支持
     *
     * @param context 上下文
     * @return 协议支持
     */
    Mono<? extends ProtocolSupport> create(ServiceContext context);

    //已弃用，请实现dispose
    @Deprecated
    default void close() {

    }

    @Override
    default void dispose() {
        close();
    }
}
