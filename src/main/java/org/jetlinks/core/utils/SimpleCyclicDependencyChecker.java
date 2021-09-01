package org.jetlinks.core.utils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
class SimpleCyclicDependencyChecker<T, ID, R> implements CyclicDependencyChecker<T, R> {

    private final Function<T, ID> idGetter;
    private final Function<T, ID> parentIdGetter;
    private final Function<ID, Mono<T>> dataGetter;
    private final Function<Set<ID>, Mono<R>> action;

    public Mono<R> check(T target) {
        return doCheck(target, new LinkedHashSet<>());
    }

    private Mono<R> doCheck(T target, Set<ID> checked) {
        ID id = idGetter.apply(target);
        if (checked.contains(id)) {
            log(target.getClass(), checked);
            return action.apply(checked);
        }
        ID parentId = parentIdGetter.apply(target);
        if (StringUtils.isEmpty(parentId)) {
            return Mono.empty();
        }
        checked.add(id);
        return dataGetter
                .apply(parentId)
                .flatMap(parent -> doCheck(parent, checked));
    }

    private void log(Class<?> type, Set<ID> checked) {

        log.warn("{} has a cyclic dependency: {}", type.getSimpleName(), checked
                .stream()
                .map(String::valueOf)
                .collect(Collectors.joining(" -> ")));
    }

}
