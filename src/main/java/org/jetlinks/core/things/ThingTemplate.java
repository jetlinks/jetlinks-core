package org.jetlinks.core.things;

import org.jetlinks.core.Configurable;
import org.jetlinks.core.Wrapper;
import reactor.core.publisher.Mono;

/**
 * 物模版,统一定义物模型等信息
 *
 * @author zhouhao
 * @since 1.1.9
 */
public interface ThingTemplate extends Configurable, Wrapper {

    String getId();

    /**
     * 获取模版物模型
     *
     * @return 物模型
     */
    Mono<? extends ThingMetadata> getMetadata();

    /**
     * 更新物模型字符串
     *
     * @param metadata 物模型
     * @return true
     */
    Mono<Boolean> updateMetadata(String metadata);

    /**
     * 更新物模型
     *
     * @param metadata 物模型
     * @return true
     */
    Mono<Boolean> updateMetadata(ThingMetadata metadata);

}
