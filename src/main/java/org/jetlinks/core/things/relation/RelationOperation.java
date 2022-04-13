package org.jetlinks.core.things.relation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface RelationOperation {

    Flux<RelatedObject> save(String type,
                             String relation,
                             Collection<String> targetId);

    Mono<RelatedObject> save(String type,
                             String relation,
                             String targetId);

    Mono<RelatedObject> get(String type,
                            String relation,
                            String targetId);

    Flux<RelatedObject> get(String type,
                            String relation,
                            String... targetId);

    Flux<RelatedObject> get(String type,
                            String relation,
                            Collection<String> targetId);

    Flux<RelatedObject> get(String type);

}
