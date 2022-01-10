package org.jetlinks.core.config;

import reactor.core.publisher.Mono;

/**
 * 配置存储管理器
 *
 * @author zhouhao
 * @since 1.0
 */
public interface ConfigStorageManager {

    /**
     * 获取配置存储器,请勿缓存ConfigStorage,但是可以缓存{@link Mono}.如:
     *
     * <pre>
     * //正确
     * private final Mono&lt;ConfigStorage&gt; storageMono;
     *
     * //不建议
     * private final ConfigStorage storage;
     * </pre>
     *
     * @param id ID标识
     * @return ConfigStorage
     */
    Mono<ConfigStorage> getStorage(String id);

}
