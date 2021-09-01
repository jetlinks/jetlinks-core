package org.jetlinks.core.utils;

import org.jetlinks.core.exception.CyclicDependencyException;
import reactor.core.publisher.Mono;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Set;
import java.util.function.Function;

/**
 * 循环依赖检查器,用于检查实体之间的循环依赖
 *
 * @param <T>
 * @author zhouhao
 * @since 1.1.8
 */
@ThreadSafe
public interface CyclicDependencyChecker<T, R> {

    /**
     * 检查实体类是否存在循环依赖,并返回检查结果
     *
     * @param target 要检查的目标对象
     * @return 检查结果
     */
    Mono<R> check(T target);

    /**
     * 创建检查器,当发生循环依赖时,将抛出{@link CyclicDependencyException}.否则返回{@link Mono#empty()}
     *
     * @param idGetter       ID getter
     * @param parentIdGetter ParentID getter
     * @param dataGetter     Data getter
     * @param <T>            对象类型
     * @param <ID>           ID类型
     * @return 检查器
     */
    static <T, ID> CyclicDependencyChecker<T, Void> of(Function<T, ID> idGetter,
                                                       Function<T, ID> parentIdGetter,
                                                       Function<ID, Mono<T>> dataGetter) {
        return of(idGetter, parentIdGetter, dataGetter, detail -> Mono.error(new CyclicDependencyException(detail)));
    }

    /**
     * 创建检查器,当发生循环依赖时,执行指定的动作.否则返回{@link Mono#empty()}
     *
     * @param idGetter       ID getter
     * @param parentIdGetter ParentID getter
     * @param dataGetter     Data getter
     * @param action         发生循环依赖时执行动作
     * @param <T>            对象类型
     * @param <ID>           ID类型
     * @return 检查器
     */
    static <T, ID, R> CyclicDependencyChecker<T, R> of(Function<T, ID> idGetter,
                                                       Function<T, ID> parentIdGetter,
                                                       Function<ID, Mono<T>> dataGetter,
                                                       Function<Set<ID>, Mono<R>> action) {
        return new SimpleCyclicDependencyChecker<>(idGetter, parentIdGetter, dataGetter, action);
    }

}
