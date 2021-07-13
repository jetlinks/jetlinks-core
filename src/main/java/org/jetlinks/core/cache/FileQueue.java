package org.jetlinks.core.cache;

import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.config.ConfigKey;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.UnicastProcessor;

import java.nio.file.Path;
import java.util.Map;
import java.util.Queue;

/**
 * 基于文件的本地队列,可使用此队列进行数据本地持久化
 *
 * @param <T> 队列元素类型
 * @see FileQueue.Builder
 * @since 1.1.7
 */
public interface FileQueue<T> extends Queue<T> {

    /**
     * 使用Builder创建FileQueue
     *
     * @param <T> 队列元素类型
     * @return Builder
     */
    static <T> Builder<T> builder() {
        return new SPIFileQueueBuilder<>();
    }

    /**
     * 关闭队列
     */
    void close();

    /**
     * 立即写出到文件
     */
    void flush();

    /**
     * 队列构造器
     *
     * @param <T>
     */
    interface Builder<T> {

        /**
         * 队列名称,不能为空
         *
         * @param name 名称
         * @return this
         */
        Builder<T> name(String name);

        /**
         * 指定队列元素编解码器，用于序列化数据
         *
         * @param codec 编解码器
         * @return this
         */
        Builder<T> codec(Codec<T> codec);

        /**
         * 指定数据文件存储路径
         *
         * @param path 存储路径
         * @return this
         */
        Builder<T> path(Path path);

        /**
         * 指定其他配置
         *
         * @param options 配置
         * @return this
         */
        Builder<T> options(Map<String, Object> options);

        /**
         * 指定其他配置
         *
         * @param key   Key
         * @param value Value
         * @return this
         */
        Builder<T> option(String key, Object value);

        /**
         * 指定其他配置
         *
         * @param key   Key
         * @param value Value
         * @return this
         */
        <V> Builder<T> option(ConfigKey<V> key, V value);

        /**
         * 构造队列
         *
         * @return 队列
         */
        FileQueue<T> build();

        /**
         * 构造一个FluxProcessor,可通过{@link FluxProcessor#onNext(Object)}写入数据
         * 通过{@link FluxProcessor#subscribe()}订阅数据,仅支持一个订阅者.
         *
         * @param clearWhenDispose 当流结束时,是否清空队列
         * @return FluxProcessor
         * @see FluxProcessor#onNext(Object)
         * @see FluxProcessor#subscribe()
         */
        default FluxProcessor<T, T> buildFluxProcessor(boolean clearWhenDispose) {
            FileQueue<T> queue = !clearWhenDispose
                    ?
                    new FileQueueProxy<T>(build()) {
                        @Override
                        public void clear() {
                            super.flush();
                        }
                    }
                    : build();

            return UnicastProcessor.create(queue, queue::close);
        }
    }
}
