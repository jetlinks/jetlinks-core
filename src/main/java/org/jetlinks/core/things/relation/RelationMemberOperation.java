package org.jetlinks.core.things.relation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface RelationMemberOperation {

    Flux<RelationMember> save(String type,
                              String relation,
                              Collection<String> targetId);

    Mono<RelationMember> save(String type,
                              String relation,
                              String targetId);

    Mono<RelationMember> get(String type,
                             String relation,
                             String targetId);

    Flux<RelationMember> get(String type,
                             String relation,
                             String... targetId);

    Flux<RelationMember> get(String type,
                             String relation,
                             Collection<String> targetId);

    Flux<RelationMember> get(String type);

}
