package org.jetlinks.core.topic;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Slf4j
class TopicRouter<T, R> implements Router<T, R> {

    private final Topic<Function<T, Publisher<R>>> root = Topic.createRoot();

    @Override
    public Router<T, R> route(String topic, Function<T, Publisher<R>> handler) {
        root.append(topic).subscribe(handler);
        return this;
    }

    @Override
    public Router<T, R> remove(String topic) {
        root.getTopic(topic).ifPresent(Topic::unsubscribeAll);
        return this;
    }

    @Override
    public Flux<Publisher<R>> execute(String topic, T data) {

        return root.findTopic(topic)
                .flatMapIterable(Topic::getSubscribers)
                .switchIfEmpty(Mono.fromRunnable(() -> {
                    log.debug("not handler for {}", topic);
                }))
                .distinct()
                .map(runner -> runner.apply(data))
                ;
    }

    @Override
    public void close() {
        root.clean();
    }
}
