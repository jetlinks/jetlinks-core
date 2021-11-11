package org.jetlinks.core.things;

import org.jetlinks.core.Configurable;
import reactor.core.publisher.Mono;

/**
 * 物模版,统一定义物模型等信息
 *
 * @author zhouhao
 * @since 1.1.9
 */
public interface ThingTemplate extends Configurable {

    String getId();

    Mono<? extends ThingMetadata> getMetadata();

    Mono<Boolean> updateMetadata(String metadata);

    Mono<Boolean> updateMetadata(ThingMetadata metadata);

    default Mono<Long> getVersion() {
        return getConfig(ThingsConfigKeys.version);
    }

    /**
     * 拆包为指定的类型
     *
     * @param type 类型
     * @param <T>  T
     * @return 指定的类型
     */
    default <T extends Thing> T unwrap(Class<T> type) {
        return type.cast(this);
    }
}
