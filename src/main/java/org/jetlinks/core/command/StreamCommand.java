package org.jetlinks.core.command;

import org.springframework.core.ResolvableType;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;

/**
 * 流式请求命令,通常用于执行大量数据请求时使用,如上传文件等.
 * <p>
 * 流式结果命令仅支持返回Flux.
 *
 * @param <E> 流元素类型
 * @param <R> 响应结果元素类型
 * @see AbstractStreamCommand
 */
public interface StreamCommand<E, R> extends Command<Flux<R>> {

    /**
     * 返回命令中的数据流
     *
     * @return 数据流
     */
    @Nonnull
    Flux<E> stream();

    /**
     * 设置数据流
     *
     * @param stream 数据流
     */
    void withStream(@Nonnull Flux<E> stream);

    /**
     * 转换对象为流中的元素
     *
     * @param value value
     * @return 流中的元素
     */
    @SuppressWarnings("unchecked")
    default E convertStreamValue(Object value) {
        return (E) CommandUtils
            .convertData(
                ResolvableType
                    .forClass(StreamCommand.class, this.getClass())
                    .getGeneric(0),
                value);
    }
}
