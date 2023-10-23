package org.jetlinks.core.topic;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.utils.RecyclableDequeue;
import org.jetlinks.core.utils.RecyclerUtils;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.jetlinks.core.utils.TopicUtils;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.function.Consumer4;
import reactor.function.Consumer5;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@EqualsAndHashCode(of = "part")
public final class Topic<T> {

    @Getter
    private final Topic<T> parent;

    private String part;

    @Setter(AccessLevel.PRIVATE)
    private volatile String[] topics;

    private final int depth;

    private volatile ConcurrentMap<String, Topic<T>> child;

    private volatile ConcurrentMap<T, AtomicInteger> subscribers;

    public static <T> Topic<T> createRoot() {
        return new Topic<>(null, "/");
    }

    public Topic<T> append(String topic) {
        if (topic == null || topic.equals("/") || topic.isEmpty()) {
            return this;
        }
        return getOrDefault(topic, Topic::new);
    }

    private Topic(Topic<T> parent, String part) {

        if (ObjectUtils.isEmpty(part) || part.equals("/")) {
            this.part = "";
        } else {
            if (part.contains("/")) {
                this.ofTopic(part);
            } else {
                setPart(part);
            }
        }
        this.parent = parent;
        if (null != parent) {
            this.depth = parent.depth + 1;
        } else {
            this.depth = 0;
        }
    }

    private void setPart(String part) {
        this.part = RecyclerUtils.intern(part);
    }

    public String[] getTopics() {
        if (topics != null) {
            return topics;
        }
        return topics = TopicUtils.split(getTopic(), true);
    }

    public String getTopic() {
        return StringBuilderUtils
            .buildString(getParent(), (_parent, builder) -> {
                if (_parent != null) {
                    String parentTopic = _parent.getTopic();
                    builder.append(parentTopic).append(parentTopic.equals("/") ? "" : "/");
                } else {
                    builder.append("/");
                }
                builder.append(part);
            });
    }

    public T getSubscriberOrSubscribe(Supplier<T> supplier) {
        if (!subscribers().isEmpty()) {
            return subscribers().keySet().iterator().next();
        }
        synchronized (this) {
            if (!subscribers().isEmpty()) {
                return subscribers().keySet().iterator().next();
            }
            T sub = supplier.get();
            subscribe(sub);
            return sub;
        }
    }

    public Set<T> getSubscribers() {
        return subscribers == null ? Collections.emptySet() : subscribers().keySet();
    }

    public boolean subscribed(T subscriber) {
        return subscribers().containsKey(subscriber);
    }

    @SafeVarargs
    public final void subscribe(T... subscribers) {
        for (T subscriber : subscribers) {
            this.subscribers()
                .computeIfAbsent(subscriber, i -> new AtomicInteger())
                .incrementAndGet();
        }
    }

    @SafeVarargs
    public final List<T> unsubscribe(T... subscribers) {
        List<T> unsub = new ArrayList<>(subscribers.length);
        for (T subscriber : subscribers) {
            this.subscribers()
                .computeIfPresent(subscriber, (k, v) -> {
                    if (v.decrementAndGet() <= 0) {
                        unsub.add(k);
                        return null;
                    }
                    return v;
                });
        }
        return unsub;
    }

    public void unsubscribe(Predicate<T> predicate) {
        ConcurrentMap<T, AtomicInteger> subscribers = this.subscribers;
        if (subscribers == null) {
            return;
        }

        for (Map.Entry<T, AtomicInteger> entry : subscribers.entrySet()) {
            if (predicate.test(entry.getKey()) && entry.getValue().decrementAndGet() <= 0) {
                subscribers.remove(entry.getKey());
            }
        }
    }

    public void unsubscribeAll() {
        if (subscribers == null) {
            return;
        }
        subscribers.clear();
    }

    public Collection<Topic<T>> getChildren() {
        if (child == null) {
            return Collections.emptyList();
        }
        return child.values();
    }

    private Map<String, Topic<T>> child() {
        if (child == null) {
            synchronized (this) {
                if (child == null) {
                    child = new ConcurrentHashMap<>();
                }
            }
        }
        return child;
    }

    private ConcurrentMap<T, AtomicInteger> subscribers() {
        if (subscribers == null) {
            synchronized (this) {
                if (subscribers == null) {
                    subscribers = new ConcurrentHashMap<>();
                }
            }
        }
        return subscribers;
    }

    private void ofTopic(String topic) {
        String[] parts = topic.split("/", 2);
        setPart(parts[0]);
        if (parts.length > 1) {
            Topic<T> part = new Topic<>(this, parts[1]);
            this.child().put(part.part, part);
        }
    }

    private Topic<T> getOrDefault(String topic, BiFunction<Topic<T>, String, Topic<T>> mapping) {
        if (topic.charAt(0) == '/') {
            topic = topic.substring(1);
        }
        String[] parts = TopicUtils.split(topic, true, true);
        Topic<T> part = child().computeIfAbsent(parts[0], _topic -> mapping.apply(this, _topic));
        for (int i = 1; i < parts.length && part != null; i++) {
            Topic<T> parent = part;
            part = part.child().computeIfAbsent(parts[i], _topic -> mapping.apply(parent, _topic));
        }
        return part;
    }

    public Optional<Topic<T>> getTopic(String topic) {
        return Optional.ofNullable(getOrDefault(topic, ((topicPart, s) -> null)));
    }

    public Flux<Topic<T>> findTopic(String topic) {
        return Flux.create(sink -> findTopic(topic, sink::next, sink::complete));
    }

    public void findTopic(String topic,
                          Consumer<Topic<T>> sink,
                          Runnable end) {
        findTopic(topic,
                  null,
                  null,
                  end,
                  sink,
                  (nil, nil2, _end, _sink, _topic) -> _sink.accept(_topic),
                  (nil, nil2, _end, _sink) -> _end.run());
    }

    public <ARG0, ARG1, ARG2, ARG3> void findTopic(String topic,
                                                   ARG0 arg0, ARG1 arg1, ARG2 arg2, ARG3 arg3,
                                                   Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink,
                                                   Consumer4<ARG0, ARG1, ARG2, ARG3> end) {
        String[] topics = TopicUtils.split(topic, true, false);

        if (topic.charAt(0) != '/') {
            String[] newTopics = new String[topics.length + 1];
            newTopics[0] = "";
            System.arraycopy(topics, 0, newTopics, 1, topics.length);
            topics = newTopics;
        }

        find(topics, this, arg0, arg1, arg2, arg3, sink, end);
    }

    @Override
    public String toString() {
        return "topic: " + getTopic()
            + ", subscribers: " + (subscribers == null ? 0 : subscribers.size())
            + ", children: " + (child == null ? 0 : child.size());
    }

    private boolean match(String[] pars) {
        return TopicUtils.match(getTopics(), pars)
            || TopicUtils.match(pars, getTopics());
    }

    public static <T, ARG0, ARG1, ARG2, ARG3> void find(
        String[] topicParts,
        Topic<T> topicPart,
        ARG0 arg0, ARG1 arg1, ARG2 arg2, ARG3 arg3,
        Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink,
        Consumer4<ARG0, ARG1, ARG2, ARG3> end) {

        RecyclableDequeue<Topic<T>> cache = RecyclerUtils.dequeue();
        try {
            cache.add(topicPart);

            String nextPart = null;

            while (!cache.isEmpty()) {
                Topic<T> part = cache.poll();
                if (part == null) {
                    break;
                }

                if (part.match(topicParts)) {
                    sink.accept(arg0, arg1, arg2, arg3, part);
                }

                Map<String, Topic<T>> child = part.child;
                if (child == null) {
                    continue;
                }
                //订阅了如 /device/**/event/*
                if (part.part.equals("**")) {
                    Topic<T> tmp = null;
                    for (int i = part.depth; i < topicParts.length; i++) {
                        tmp = child.get(topicParts[i]);
                        if (tmp != null) {
                            cache.add(tmp);
                        }
                    }
                    if (null != tmp) {
                        continue;
                    }
                }
                if ("**".equals(nextPart) || "*".equals(nextPart)) {
                    cache.addAll(child.values());
                    continue;
                }
                Topic<T> next = child.get("**");
                if (next != null) {
                    cache.add(next);
                }
                next = child.get("*");
                if (next != null) {
                    cache.add(next);
                }

                if (part.depth + 1 >= topicParts.length) {
                    continue;
                }
                nextPart = topicParts[part.depth + 1];
                if (nextPart.equals("*") || nextPart.equals("**")) {
                    cache.addAll(child.values());
                    continue;
                }
                next = child.get(nextPart);
                if (next != null) {
                    cache.add(next);
                }
            }

        } finally {
            end.accept(arg0, arg1, arg2, arg3);
            cache.recycle();
        }
    }

    public long getTotalTopic() {
        long total = child == null ? 0 : child().size();
        for (Topic<T> tTopic : getChildren()) {
            total += tTopic.getTotalTopic();
        }
        return total;
    }

    public long getTotalSubscriber() {
        long total = subscribers == null ? 0 : subscribers().size();
        for (Topic<T> tTopic : getChildren()) {
            total += tTopic.getTotalTopic();
        }
        return total;
    }

    public Flux<Topic<T>> getAllSubscriber() {
        return Flux.create(sink -> {
            walkChildren(sink);
            sink.complete();
        });
    }

    private void walkChildren(FluxSink<Topic<T>> sink) {
        for (Topic<T> tTopic : this.getChildren()) {
            if (sink.isCancelled()) {
                break;
            }
            sink.next(tTopic);
            tTopic.walkChildren(sink);
        }
    }

    public void clean() {
        unsubscribeAll();
        if (child != null) {
            child.values().forEach(Topic::clean);
            child().clear();
        }
    }

}
