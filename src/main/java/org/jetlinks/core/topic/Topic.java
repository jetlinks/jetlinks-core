package org.jetlinks.core.topic;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

@EqualsAndHashCode(of = "part")
public final class Topic<T> {

    @Getter
    private final Topic<T> parent;

    @Setter(AccessLevel.PRIVATE)
    private String part;

    @Setter(AccessLevel.PRIVATE)
    private volatile String topic;

    private final int depth;

    private final ConcurrentMap<String, Topic<T>> child = new ConcurrentHashMap<>();

    private final ConcurrentMap<T, AtomicInteger> subscribers = new ConcurrentHashMap<>();

    private static final AntPathMatcher matcher = new AntPathMatcher();

    public static <T> Topic<T> createRoot() {
        return new Topic<>(null, "/");
    }

    public Topic<T> append(String topic) {
        return getOrDefault(topic, Topic::new);
    }

    private Topic(Topic<T> parent, String part) {

        if (StringUtils.isEmpty(part) || part.equals("/")) {
            this.part = "";
        } else {
            if (part.contains("/")) {
                this.ofTopic(part);
            } else {
                this.part = part;
            }
        }
        this.parent = parent;
        if (null != parent) {
            this.depth = parent.depth + 1;
        } else {
            this.depth = 0;
        }
    }

    public String getTopic() {
        if (topic == null) {
            Topic<T> parent = getParent();
            StringBuilder builder = new StringBuilder();
            if (parent != null) {
                String parentTopic = parent.getTopic();
                builder.append(parentTopic).append(parentTopic.equals("/") ? "" : "/");
            } else {
                builder.append("/");
            }
            return topic = builder.append(part).toString();
        }
        return topic;
    }

    public T getSubscriberOrSubscribe(Supplier<T> supplier) {
        if (subscribers.size() > 0) {
            return subscribers.keySet().iterator().next();
        }
        synchronized (this) {
            if (subscribers.size() > 0) {
                return subscribers.keySet().iterator().next();
            }
            T sub = supplier.get();
            subscribe(sub);
            return sub;
        }
    }

    public Set<T> getSubscribers() {
        return subscribers.keySet();
    }

    public boolean subscribed(T subscriber) {
        return subscribers.containsKey(subscriber);
    }

    @SafeVarargs
    public final void subscribe(T... subscribers) {
        for (T subscriber : subscribers) {
            this.subscribers.computeIfAbsent(subscriber, i -> new AtomicInteger()).incrementAndGet();
        }
    }

    @SafeVarargs
    public final List<T> unsubscribe(T... subscribers) {
        List<T> unsub = new ArrayList<>();
        for (T subscriber : subscribers) {
            this.subscribers.computeIfPresent(subscriber, (k, v) -> {
                if (v.decrementAndGet() <= 0) {
                    unsub.add(k);
                    return null;
                }
                return v;
            });
        }
        return unsub;
    }

    public final void unsubscribe(Predicate<T> predicate) {
        for (Map.Entry<T, AtomicInteger> entry : this.subscribers.entrySet()) {
            if (predicate.test(entry.getKey()) && entry.getValue().decrementAndGet() <= 0) {
                this.subscribers.remove(entry.getKey());
            }
        }
    }

    public final void unsubscribeAll() {
        this.subscribers.clear();
    }

    public Collection<Topic<T>> getChildren() {
        return child.values();
    }

    private void ofTopic(String topic) {
        String[] parts = topic.split("[/]", 2);
        this.part = parts[0];
        if (parts.length > 1) {
            Topic<T> part = new Topic<>(this, parts[1]);
            this.child.put(part.part, part);
        }
    }

    private Topic<T> getOrDefault(String topic, BiFunction<Topic<T>, String, Topic<T>> mapping) {
        if (topic.startsWith("/")) {
            topic = topic.substring(1);
        }
        String[] parts = topic.split("[/]");
        Topic<T> part = child.computeIfAbsent(parts[0], _topic -> mapping.apply(this, _topic));
        for (int i = 1; i < parts.length && part != null; i++) {
            Topic<T> parent = part;
            part = part.child.computeIfAbsent(parts[i], _topic -> mapping.apply(parent, _topic));
        }
        return part;
    }

    public Optional<Topic<T>> getTopic(String topic) {
        return Optional.ofNullable(getOrDefault(topic, ((topicPart, s) -> null)));
    }

    public Flux<Topic<T>> findTopic(String topic) {
        if (!topic.startsWith("/")) {
            topic = "/" + topic;
        }
        return find(topic, this);
    }

    @Override
    public String toString() {
        return "topic: " + getTopic() + ", subscribers: " + subscribers.size() + ", children: " + child.size();
    }

    public static <T> Flux<Topic<T>> find(String topic, Topic<T> topicPart) {
        return Flux.create(sink -> {
            ArrayDeque<Topic<T>> cache = new ArrayDeque<>();
            cache.add(topicPart);

            String[] topicParts = topic.split("[/]");
            String nextPart = null;
            while (!cache.isEmpty() && !sink.isCancelled()) {
                Topic<T> part = cache.poll();
                if (part == null) {
                    break;
                }
                if (part.part.equals("**")
                        || matcher.match(part.getTopic(), topic)
                        || (matcher.match(topic, part.getTopic()))) {
                    sink.next(part);
                }

                //订阅了如 /device/**/event/*
                if (part.part.equals("**")) {
                    Topic<T> tmp = null;
                    for (int i = part.depth; i < topicParts.length; i++) {
                        tmp = part.child.get(topicParts[i]);
                        if (tmp != null) {
                            cache.add(tmp);
                        }
                    }
                    if (null != tmp) {
                        continue;
                    }
                }
                if ("**".equals(nextPart) || "*".equals(nextPart)) {
                    cache.addAll(part.child.values());
                    continue;
                }
                Topic<T> next = part.child.get("**");
                if (next != null) {
                    cache.add(next);
                }
                next = part.child.get("*");
                if (next != null) {
                    cache.add(next);
                }

                if (part.depth + 1 >= topicParts.length) {
                    continue;
                }
                nextPart = topicParts[part.depth + 1];
                if (nextPart.equals("*") || nextPart.equals("**")) {
                    cache.addAll(part.child.values());
                    continue;
                }
                next = part.child.get(nextPart);
                if (next != null) {
                    cache.add(next);
                }

            }
            sink.complete();
        });

    }

    public long getTotalTopic() {
        long total = child.size();
        for (Topic<T> tTopic : getChildren()) {
            total += tTopic.getTotalTopic();
        }
        return total;
    }

    public long getTotalSubscriber() {
        long total = subscribers.size();
        for (Topic<T> tTopic : getChildren()) {
            total += tTopic.getTotalTopic();
        }
        return total;
    }

    public Flux<Topic<T>> getAllSubscriber() {
        List<Flux<Topic<T>>> all = new ArrayList<>();

        all.add(Flux.fromIterable(this.getChildren()));

        for (Topic<T> tTopic : getChildren()) {
            all.add(tTopic.getAllSubscriber());
        }
        return Flux.concat(all);
    }

    public void clean() {
        unsubscribeAll();
        getChildren().forEach(Topic::clean);
        child.clear();
    }

}
