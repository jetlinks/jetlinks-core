package org.jetlinks.core.cache;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.config.ConfigKey;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * 可拓展的{@link FileQueue.Builder}实现,利用jdk spi功能进行自定义拓展:
 *
 * <pre>
 *     --resources
 *     ---|---META-INF
 *     ---|-----|--services
 *     ---|-----|-----|---org.jetlinks.core.cache.FileQueueBuilderFactory
 * </pre>
 *
 * @param <T> Queue元素类型
 * @see FileQueueBuilderFactory
 * @see FileQueue.Builder
 * @since 1.1.7
 */
@Slf4j
class SPIFileQueueBuilder<T> implements FileQueue.Builder<T> {

    final FileQueue.Builder<T> builder;

    private static final FileQueueBuilderFactory factory;

    static {
        ServiceLoader<FileQueueBuilderFactory> loader = ServiceLoader.load(FileQueueBuilderFactory.class, SPIFileQueueBuilder.class
                .getClassLoader());
        Iterator<FileQueueBuilderFactory> iterator = loader.iterator();
        if (!iterator.hasNext()) {
            SPIFileQueueBuilder.log.warn("Cant not load service [FileQueueBuilderFactory]");
            factory = new FileQueueBuilderFactory() {
                @Override
                @SuppressWarnings("all")
                public <T> FileQueue.Builder<T> create() {
                    throw new UnsupportedOperationException("unsupported service FileQueueBuilderFactory");
                }
            };
        } else {
            factory = iterator.next();
            SPIFileQueueBuilder.log.debug("Load service [FileQueueBuilderFactory] : [{}]", factory.getClass());
        }
    }

    SPIFileQueueBuilder() {
        this.builder = factory.create();
    }

    @Override
    public FileQueue<T> build() {
        return builder.build();
    }

    @Override
    @Deprecated
    public FileQueue.Builder<T> codec(Codec<T> codec) {
        return builder.codec(codec);
    }

    @Override
    public FileQueue.Builder<T> path(Path path) {
        return builder.path(path);
    }

    @Override
    public FileQueue.Builder<T> options(Map<String, Object> options) {
        return builder.options(options);
    }

    @Override
    public FileQueue.Builder<T> option(String key, Object value) {
        return builder.option(key, value);
    }

    @Override
    public <V> FileQueue.Builder<T> option(ConfigKey<V> key, V value) {
        return builder.option(key, value);
    }

    @Override
    public FileQueue.Builder<T> name(String name) {
        return builder.name(name);
    }
}
