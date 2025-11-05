package org.jetlinks.core.command.blocking;

import org.jetlinks.core.Wrapper;
import org.jetlinks.core.command.Command;
import org.jetlinks.core.command.CommandSupport;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 阻塞方式的命令支持
 * <p>
 * 注意: 请勿在非阻塞环境中使用.
 *
 * @author zhouhao
 * @since 1.3.1
 */
public interface BlockingCommandSupport extends CommandSupport, Wrapper {

    /**
     * 包装一个命令支持为阻塞式的命令支持
     *
     * @param target 命令支持
     * @return BlockingCommandSupport
     */
    static BlockingCommandSupport of(CommandSupport target) {
        return new DefaultBlockingCommandSupport(target);
    }

    /**
     * 包装一个命令支持为阻塞式的命令支持
     *
     * @param target 命令支持
     * @return BlockingCommandSupport
     */
    static BlockingCommandSupport of(Mono<CommandSupport> target) {
        return new AsyncBlockingCommandSupport(target);
    }

    /**
     * 执行响应式命令并阻塞获取单个执行结果
     *
     * @param commandId  命令ID
     * @param parameters 参数
     * @return 执行结果
     */
    default Optional<Object> executeToSingle(String commandId, Map<String, Object> parameters) {
        return this
            .executeToMono(commandId, parameters)
            .contextCapture()
            .blockOptional();
    }

    /**
     * 执行响应式命令并阻塞获取单个执行结果
     *
     * @param command 命令
     * @param <T>     结果类型
     * @return 执行结果
     */
    default <T> Optional<T> executeToSingle(Command<? extends Mono<T>> command) {
        return this
            .execute(command)
            .contextCapture()
            .blockOptional();
    }


    /**
     * 执行响应式命令并阻塞获取结果集合
     *
     * @param commandId  命令ID
     * @param parameters 参数
     * @return 执行结果
     */
    default List<Object> executeToList(String commandId, Map<String, Object> parameters) {
        return this
            .executeToFlux(commandId, parameters)
            .collectList()
            .contextCapture()
            .block();
    }


    /**
     * 执行响应式命令并阻塞获取结果集合
     *
     * @param command 命令
     * @param <T>     结果类型
     * @return 执行结果
     */
    default <T> List<T> executeToList(Command<? extends Flux<T>> command) {
        return this
            .execute(command)
            .collectList()
            .contextCapture()
            .block();
    }

    /**
     * 执行响应式命令并阻塞获取结果集合
     *
     * @param commandId  命令ID
     * @param parameters 参数
     * @return 执行结果
     */
    default Stream<Object> executeToStream(String commandId, Map<String, Object> parameters) {
        return this
            .executeToFlux(commandId, parameters)
            .contextCapture()
            .toStream();
    }

    /**
     * 执行响应式命令并阻塞获取结果流
     *
     * @param command 命令
     * @param <T>     结果类型
     * @return 执行结果
     */
    default <T> Stream<T> executeToStream(Command<? extends Flux<T>> command) {
        return this
            .execute(command)
            .contextCapture()
            .toStream();
    }


}
