package org.jetlinks.core.topic;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.utils.RecyclableDequeue;
import org.jetlinks.core.utils.RecyclerUtils;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.jetlinks.core.utils.TopicUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.function.Consumer4;
import reactor.function.Consumer5;

import javax.annotation.Nonnull;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectOutput;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.*;

public final class Topic<T> implements SeparatedCharSequence {

    private int $hash;

    @Getter
    private final Topic<T> parent;

    private String part;

    @Setter(AccessLevel.PRIVATE)
    private volatile SharedPathString topics;

    private final int depth;

    private volatile ConcurrentMap<String, Topic<T>> child;

    private volatile ConcurrentMap<T, Integer> subscribers;

    public static <T> Topic<T> createRoot() {
        return new Topic<>(null, "/");
    }

    public Topic<T> append(String topic) {
        if (topic == null || topic.equals("/") || topic.isEmpty()) {
            return this;
        }
        return getOrDefault(topic, Topic::new);
    }

    public Topic<T> append(String[] topic) {
        if (topic == null || topic.length == 0) {
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

    private String[] getTopicsUnsafe() {
        return topic().unsafeSeparated();
    }

    public String getTopic() {
        return getTopic0();
    }

    private SharedPathString topic() {
        if (topics == null) {
            topics = SharedPathString.of(getTopic0()).intern();
        }
        return topics;
    }

    private String getTopic0() {
        return StringBuilderUtils
            .buildString(this, (_that, builder) -> {

                Topic<T> _temp = _that;
                do {
                    builder.insert(0, _temp.part);

                    if (!_temp.part.isEmpty()) {
                        builder.insert(0, "/");
                    }

                    _temp = _temp.parent;

                } while (_temp != null);

            });
    }

    @Deprecated
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
        return subscribers != null && subscribers().containsKey(subscriber);
    }

    @SafeVarargs
    public final void subscribe(T... subscribers) {
        for (T subscriber : subscribers) {
            subscribe0(subscriber);
        }
    }


    public void subscribe0(T subscriber) {
        this.subscribers()
            .compute(subscriber, (ignore, i) -> i == null ? 1 : i + 1);
    }

    public void subscribe0(T subscriber, boolean replace) {
        if (replace) {
            this.subscribers().put(subscriber, 1);
            return;
        }
        subscribe0(subscriber);
    }

    @SafeVarargs
    public final List<T> unsubscribe(T... subscribers) {
        List<T> unsub = new ArrayList<>(subscribers.length);
        for (T subscriber : subscribers) {
            if (unsubscribe0(subscriber)) {
                unsub.add(subscriber);
            }
        }
        return unsub;
    }

    public boolean unsubscribe0(T subscriber, boolean all) {
        if (all) {
            return this.subscribers().remove(subscriber) != null;
        }
        return unsubscribe0(subscriber);
    }

    public boolean unsubscribe0(T subscriber) {
        return this
            .subscribers()
            .compute(
                subscriber,
                (k, v) -> {
                    if (v == null || v - 1 <= 0) {
                        return null;
                    }
                    return v - 1;
                })
            == null;
    }

    public void unsubscribe(Predicate<T> predicate) {
        ConcurrentMap<T, Integer> subscribers = this.subscribers;
        if (subscribers == null) {
            return;
        }

        for (T t : subscribers.keySet()) {
            if (predicate.test(t)) {
                unsubscribe0(t);
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
                    child = new ConcurrentHashMap<>(1);
                }
            }
        }
        return child;
    }

    private ConcurrentMap<T, Integer> subscribers() {
        if (subscribers == null) {
            synchronized (this) {
                if (subscribers == null) {
                    subscribers = new ConcurrentHashMap<>(1);
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

    private Topic<T> getOrDefault(String[] parts, BiFunction<Topic<T>, String, Topic<T>> mapping) {
        int index = 0;
        if (parts[0].isEmpty()) {
            index = 1;
        }
        Topic<T> part = child().computeIfAbsent(parts[index], _topic -> mapping.apply(this, _topic));
        for (int i = index + 1; i < parts.length && part != null; i++) {
            Topic<T> parent = part;
            part = part.child().computeIfAbsent(parts[i], _topic -> mapping.apply(parent, _topic));
        }
        return part;
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

    public Optional<Topic<T>> getTopic(String[] topic) {
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

    public <A> void findTopic(CharSequence topic,
                              A arg1,
                              BiConsumer<A, Topic<T>> sink,
                              Consumer<A> end) {
        if (topic instanceof SeparatedCharSequence) {
            find((SeparatedCharSequence) topic,
                 this,
                 arg1,
                 null,
                 end,
                 sink,
                 (a1, nil2, _end, _sink, _topic) ->
                     _sink.accept(a1, _topic),
                 (a1, nil2, _end, _sink) -> _end.accept(a1));
        } else {
            findTopic(topic.toString(),
                      arg1,
                      null,
                      end,
                      sink,
                      (a1, nil2, _end, _sink, _topic) ->
                          _sink.accept(a1, _topic),
                      (a1, nil2, _end, _sink) ->
                          _end.accept(a1));
        }
    }

    public void findTopic(CharSequence topic,
                          Consumer<Topic<T>> sink,
                          Runnable end) {

        if (topic instanceof SeparatedCharSequence) {
            find((SeparatedCharSequence) topic,
                 this,
                 null,
                 null,
                 end,
                 sink,
                 (nil, nil2, _end, _sink, _topic) -> _sink.accept(_topic),
                 (nil, nil2, _end, _sink) -> _end.run());
        } else {
            findTopic(topic.toString(),
                      null,
                      null,
                      end,
                      sink,
                      (nil, nil2, _end, _sink, _topic) -> _sink.accept(_topic),
                      (nil, nil2, _end, _sink) -> _end.run());
        }
    }

    public <ARG0, ARG1, ARG2, ARG3> void findTopic(String topic,
                                                   ARG0 arg0, ARG1 arg1, ARG2 arg2, ARG3 arg3,
                                                   Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink,
                                                   Consumer4<ARG0, ARG1, ARG2, ARG3> end) {
        String[] topics = TopicUtils.split(topic, false, false);

        if (topic.charAt(0) != '/') {
            String[] newTopics = new String[topics.length + 1];
            newTopics[0] = "";
            System.arraycopy(topics, 0, newTopics, 1, topics.length);
            topics = newTopics;
        }

        find(topics, this, arg0, arg1, arg2, arg3, sink, end);
    }

    @Override
    public char separator() {
        return '/';
    }

    @Override
    public int size() {
        return depth + 1;
    }

    @Override
    public CharSequence get(int index) {
        Topic<T> topic = this;
        while (topic.depth != index) {
            topic = topic.parent;
            if (topic == null) {
                throw new StringIndexOutOfBoundsException(index);
            }
        }
        return topic.part;
    }

    @Override
    public SeparatedCharSequence replace(int index, CharSequence newChar) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SeparatedCharSequence append(char c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SeparatedCharSequence append(CharSequence csq) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SeparatedCharSequence append(CharSequence... csq) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SeparatedCharSequence append(CharSequence csq, int start, int end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SeparatedCharSequence range(int start, int end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SeparatedCharSequence intern() {
        return internInner();
    }

    @Override
    public int length() {
        int len = 0;
        Topic<T> topic = this;
        while (topic != null) {
            len += topic.part.length();
            topic = topic.parent;
        }
        return len;
    }

    @Override
    public char charAt(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public String toString() {
        return "topic: " + getTopic()
            + ", subscribers: " + (subscribers == null ? 0 : subscribers.size())
            + ", children: " + (child == null ? 0 : child.size());
    }

    private boolean match(String[] pars) {
        String[] parts = getTopicsUnsafe();
        return TopicUtils.match(parts, pars)
            || TopicUtils.match(pars, parts);
    }

    private boolean match(SeparatedCharSequence parts) {
        SeparatedCharSequence self = topics != null ? topics : this;
        return TopicUtils.match(parts, self)
            || TopicUtils.match(self, parts);
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

    public static <T, ARG0, ARG1, ARG2, ARG3> void find(
        SeparatedCharSequence topicParts,
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
                int partsSize = topicParts.size();
                //订阅了如 /device/**/event/*
                if (part.part.equals("**")) {
                    Topic<T> tmp = null;
                    for (int i = part.depth; i < partsSize; i++) {
                        tmp = child.get(topicParts.get(i).toString());
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

                if (part.depth + 1 >= partsSize) {
                    continue;
                }
                nextPart = topicParts.get(part.depth + 1).toString();
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
        Map<?, ?> child = this.child;
        long total = child == null ? 0 : child.size();
        for (Topic<T> tTopic : getChildren()) {
            total += tTopic.getTotalTopic();
        }
        return total;
    }

    public long getTotalSubscriber() {
        Map<?, ?> subscribers = this.subscribers;
        long total = subscribers == null ? 0 : subscribers.size();
        for (Topic<T> tTopic : getChildren()) {
            total += tTopic.getTotalSubscriber();
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

    public boolean cleanup(BiConsumer<Boolean, Topic<T>> handler) {
        //清理订阅者
        if (subscribers != null && subscribers.isEmpty()) {
            synchronized (this) {
                if (subscribers.isEmpty()) {
                    subscribers = null;
                }
            }
        }
        //清理子节点
        if (child != null) {
            for (Map.Entry<String, Topic<T>> children : child.entrySet()) {
                Topic<T> topic = children.getValue();
                boolean cleaned = topic.cleanup(handler);
                if (cleaned) {
                    child.remove(children.getKey());
                }
                if (handler != null) {
                    handler.accept(cleaned, topic);
                }
            }

            if (child != null && child.isEmpty()) {
                synchronized (this) {
                    if (child.isEmpty()) {
                        child = null;
                    }
                }
            }
        }
        return CollectionUtils.isEmpty(subscribers) &&
            CollectionUtils.isEmpty(child);
    }

    public boolean cleanup() {
        return cleanup(null);
    }

    public void clean() {
        unsubscribeAll();
        if (child != null) {
            child.values().forEach(Topic::clean);
            child().clear();
        }
    }

    @Override
    public int compareTo(@Nonnull SeparatedCharSequence obj) {
        if (this == obj) {
            return 0;
        }
        if (!(obj instanceof Topic)) {
            return this.getTopic().compareTo(obj.toString());
        }

        Topic<?> left = ((Topic<?>) obj);
        Topic<?> right = this;

        if (left.depth != right.depth) {
            return Integer.compare(left.depth, right.depth);
        }

        while (left != null && right != null) {
            int compare = left.part.compareTo(right.part);
            if (compare != 0) {
                return compare;
            }
            left = left.parent;
            right = right.parent;
        }
        return 0;
    }

    @Override
    public Topic<T> internInner() {
        this.part = RecyclerUtils.intern(this.part);
        SharedPathString topics = this.topics;
        if (topics != null) {
            topics.internInner();
        }
        return this;
    }

    @Override
    public int hashCode() {
        if ($hash == 0) {
            Topic<T> t = this;
            while (t != null) {
                $hash = 31 * $hash + t.part.hashCode();
                t = t.parent;
            }
        }
        return $hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Topic)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        Topic<?> left = ((Topic<?>) obj);
        Topic<?> right = this;

        while (left != null && right != null) {
            if (left.depth != right.depth || !Objects.equals(left.part, right.part)) {
                return false;
            }
            left = left.parent;
            right = right.parent;
        }

        return left == null && right == null;
    }

    @Override
    public String[] asStringArray() {
        String[] arr = new String[depth + 1];
        Topic<T> topic = this;
        for (int i = arr.length - 1; i >= 0 && topic != null; i--) {
            arr[i] = topic.part;
            topic = topic.parent;
        }
        return arr;
    }

    public void writeTo(DataOutput output) throws IOException {
        int size = this.depth + 1;
        output.writeShort(size);
        Topic<T> topic = this;
        for (int i = 0; i < size && topic != null; i++) {
            output.writeUTF(topic.part);
            topic = topic.parent;
        }
    }

    public static String[] readArray(DataInput input) throws IOException {
        int len = input.readUnsignedShort();
        String[] arr = new String[len];
        for (int i = arr.length - 1; i >= 0; i--) {
            arr[i] = input.readUTF();
        }
        return arr;
    }
}
