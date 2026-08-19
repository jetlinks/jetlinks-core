package org.jetlinks.core.topic;

import com.google.common.collect.Collections2;
import lombok.*;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.web.recycler.Recyclable;
import org.hswebframework.web.recycler.Recycler;
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
import reactor.function.Consumer3;
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
    static final Recycler<Deque<Topic<?>>> SHARED_QUEUE =
        Recycler.create(ArrayDeque::new, Collection::clear, 256);

    private static final ConcurrentMap<?, Integer> DETACHED = new ConcurrentHashMap<>(0);

    @Getter
    private final Topic<T> parent;

    private String part;

    private final int depth;

    private volatile ConcurrentMap<String, Topic<T>> child;

    private volatile Topic<T> starChild;

    private volatile Topic<T> doubleStarChild;

    // DETACHED 复用订阅字段标记已移除节点，避免为每个 Topic 增加生命周期字段。
    private volatile ConcurrentMap<T, Integer> subscribers;

    public static <T> Topic<T> createRoot() {
        return new Topic<>(null, "/");
    }

    public Topic<T> append(String topic) {
        if (topic == null || topic.equals("/") || topic.isEmpty()) {
            return this;
        }
        return append(TopicUtils.split(topic, true, true));
    }

    public Topic<T> append(String[] topic) {
        if (topic == null || topic.length == 0) {
            return this;
        }
        int index = topic[0].isEmpty() ? 1 : 0;
        Topic<T> part = this;
        for (int i = index; i < topic.length; i++) {
            part = part.appendChild(topic[i]);
        }
        return part;
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

    public String getPart() {
        return part;
    }

    public Map<String, Topic<T>> getChildrenMap() {
        return child;
    }

    Topic<T> getStarChild() {
        return starChild;
    }

    Topic<T> getDoubleStarChild() {
        return doubleStarChild;
    }

    @SuppressWarnings("unchecked")
    private static <T> ConcurrentMap<T, Integer> detachedMarker() {
        return (ConcurrentMap<T, Integer>) DETACHED;
    }

    public String getTopic() {
        return SharedPathString.of(asStringArray()).toString();
    }

    @Deprecated
    public T getSubscriberOrSubscribe(Supplier<T> supplier) {
        Topic<T> current = this;
        for (; ; ) {
            ConcurrentMap<T, Integer> subscribers = current.subscribers;
            if (!current.hasDetachedAncestor() && subscribers != null && !subscribers.isEmpty()) {
                return subscribers.keySet().iterator().next();
            }
            synchronized (current) {
                if (!current.hasDetachedAncestor()) {
                    subscribers = current.subscribers;
                    if (subscribers != null && !subscribers.isEmpty()) {
                        return subscribers.keySet().iterator().next();
                    }
                    T sub = supplier.get();
                    current.subscribersLocked().put(sub, 1);
                    return sub;
                }
            }
            current = current.resolveCurrent();
        }
    }

    public Set<T> getSubscribers() {
        Topic<T> current = hasDetachedAncestor() ? findCurrent() : this;
        if (current == null) {
            return Collections.emptySet();
        }
        ConcurrentMap<T, Integer> subscribers = current.subscribers;
        return subscribers == null ? Collections.emptySet() : subscribers.keySet();
    }

    public boolean subscribed(T subscriber) {
        Topic<T> current = hasDetachedAncestor() ? findCurrent() : this;
        if (current == null) {
            return false;
        }
        ConcurrentMap<T, Integer> subscribers = current.subscribers;
        return subscribers != null && subscribers.containsKey(subscriber);
    }

    @SafeVarargs
    public final void subscribe(T... subscribers) {
        for (T subscriber : subscribers) {
            subscribe0(subscriber);
        }
    }


    public void subscribe0(T subscriber) {
        Topic<T> current = this;
        for (; ; ) {
            synchronized (current) {
                if (!current.hasDetachedAncestor()) {
                    current
                        .subscribersLocked()
                        .compute(subscriber, (ignore, i) -> i == null ? 1 : i + 1);
                    return;
                }
            }
            current = current.resolveCurrent();
        }
    }

    public void subscribe0(T subscriber, boolean replace) {
        Topic<T> current = this;
        for (; ; ) {
            synchronized (current) {
                if (!current.hasDetachedAncestor()) {
                    if (replace) {
                        current.subscribersLocked().put(subscriber, 1);
                        return;
                    }
                    current
                        .subscribersLocked()
                        .compute(subscriber, (ignore, i) -> i == null ? 1 : i + 1);
                    return;
                }
            }
            current = current.resolveCurrent();
        }
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
        Topic<T> current = this;
        while (current != null) {
            synchronized (current) {
                if (!current.hasDetachedAncestor()) {
                    if (all) {
                        ConcurrentMap<T, Integer> subscribers = current.subscribers;
                        return subscribers != null && subscribers.remove(subscriber) != null;
                    }
                    return current.unsubscribeLocked(subscriber);
                }
            }
            current = current.findCurrent();
        }
        return !all;
    }

    public boolean unsubscribe0(T subscriber) {
        Topic<T> current = this;
        while (current != null) {
            synchronized (current) {
                if (!current.hasDetachedAncestor()) {
                    return current.unsubscribeLocked(subscriber);
                }
            }
            current = current.findCurrent();
        }
        return true;
    }

    private boolean unsubscribeLocked(T subscriber) {
        ConcurrentMap<T, Integer> subscribers = this.subscribers;
        if (subscribers == null) {
            return true;
        }
        return subscribers
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
        Topic<T> current = hasDetachedAncestor() ? findCurrent() : this;
        if (current == null) {
            return;
        }
        ConcurrentMap<T, Integer> subscribers = current.subscribers;
        if (subscribers == null) {
            return;
        }

        for (T t : subscribers.keySet()) {
            if (predicate.test(t)) {
                current.unsubscribe0(t);
            }
        }

    }

    public void unsubscribeAll() {
        Topic<T> current = this;
        while (current != null) {
            synchronized (current) {
                if (!current.hasDetachedAncestor()) {
                    if (current.subscribers != null) {
                        current.subscribers.clear();
                    }
                    return;
                }
            }
            current = current.findCurrent();
        }
    }

    public Collection<Topic<T>> getChildren() {
        if (child == null) {
            return Collections.emptyList();
        }
        return child.values();
    }

    private ConcurrentMap<String, Topic<T>> childLocked() {
        ConcurrentMap<String, Topic<T>> children = child;
        if (children == null) {
            child = children = new ConcurrentHashMap<>(1);
        }
        return children;
    }

    private ConcurrentMap<T, Integer> subscribersLocked() {
        ConcurrentMap<T, Integer> subscribers = this.subscribers;
        if (subscribers == null) {
            this.subscribers = subscribers = new ConcurrentHashMap<>(1);
        }
        return subscribers;
    }

    private Topic<T> appendChild(String part) {
        Topic<T> current = this;
        for (; ; ) {
            synchronized (current) {
                if (!current.hasDetachedAncestor()) {
                    Topic<T> parent = current;
                    Topic<T> child = current
                        .childLocked()
                        .computeIfAbsent(part, key -> new Topic<>(parent, key));
                    current.updateWildcardChild(child, false);
                    return child;
                }
            }
            current = current.resolveCurrent();
        }
    }

    private boolean hasDetachedAncestor() {
        Topic<T> current = this;
        while (current != null) {
            if (current.subscribers == DETACHED) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }

    private Topic<T> resolveCurrent() {
        Topic<T> root = this;
        while (root.parent != null) {
            root = root.parent;
        }
        return root.append(asStringArray());
    }

    private Topic<T> findCurrent() {
        Topic<T> root = this;
        while (root.parent != null) {
            root = root.parent;
        }
        return root.getTopic(asStringArray()).orElse(null);
    }

    private void updateWildcardChild(Topic<T> child, boolean remove) {
        String part = child.part;
        boolean star = part.length() == 1 && part.charAt(0) == '*';
        boolean doubleStar = part.length() == 2 && part.charAt(0) == '*' && part.charAt(1) == '*';
        if (star) {
            if (remove) {
                if (starChild == child) {
                    starChild = null;
                }
            } else {
                starChild = child;
            }
        } else if (doubleStar) {
            if (remove) {
                if (doubleStarChild == child) {
                    doubleStarChild = null;
                }
            } else {
                doubleStarChild = child;
            }
        }
    }

    private void ofTopic(String topic) {
        String[] parts = topic.split("/", 2);
        setPart(parts[0]);
        if (parts.length > 1) {
            Topic<T> part = new Topic<>(this, parts[1]);
            this.childLocked().put(part.part, part);
            updateWildcardChild(part, false);
        }
    }

    public Optional<Topic<T>> getTopic(String topic) {
        return getTopic(TopicUtils.split(topic, true, true));
    }

    public Optional<Topic<T>> getTopic(String[] topic) {
        if (topic == null || topic.length == 0) {
            return Optional.of(this);
        }
        int index = topic[0].isEmpty() ? 1 : 0;
        Topic<T> current = this;
        for (int i = index; i < topic.length; i++) {
            ConcurrentMap<String, Topic<T>> children = current.child;
            if (children == null) {
                return Optional.empty();
            }
            current = children.get(topic[i]);
            if (current == null) {
                return Optional.empty();
            }
        }
        return Optional.of(current);
    }

    public Flux<Topic<T>> findTopic(String topic) {
        return Flux.create(sink -> findTopic(topic, sink::next, sink::complete));
    }

    public void findTopic(String topic,
                          Consumer<Topic<T>> sink,
                          Runnable end) {
        TopicFinder.find(this, topic, sink, end);
    }

    public <A> void findTopic(CharSequence topic,
                              A arg1,
                              BiConsumer<A, Topic<T>> sink,
                              Consumer<A> end) {
        TopicFinder.find(this, topic, arg1, sink, end);
    }

    public <A, B> void findTopic(CharSequence topic,
                                 A arg1,
                                 B arg2,
                                 Consumer3<A, B, Topic<T>> sink,
                                 BiConsumer<A, B> end) {
        TopicFinder.find(this, topic, arg1, arg2, sink, end);
    }

    public void findTopic(CharSequence topic,
                          Consumer<Topic<T>> sink,
                          Runnable end) {
        TopicFinder.find(this, topic, sink, end);
    }

    public <ARG0, ARG1, ARG2, ARG3> void findTopic(SeparatedCharSequence topic,
                                                   ARG0 arg0, ARG1 arg1, ARG2 arg2, ARG3 arg3,
                                                   Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink,
                                                   Consumer4<ARG0, ARG1, ARG2, ARG3> end) {
        TopicFinder.find(this, topic, arg0, arg1, arg2, arg3, sink, end);
    }

    public <ARG0, ARG1, ARG2, ARG3> void findTopic(String topic,
                                                   ARG0 arg0, ARG1 arg1, ARG2 arg2, ARG3 arg3,
                                                   Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink,
                                                   Consumer4<ARG0, ARG1, ARG2, ARG3> end) {
        TopicFinder.find(this, topic, arg0, arg1, arg2, arg3, sink, end);
    }


    @SneakyThrows
    public static <T, ARG0, ARG1, ARG2, ARG3> void find(
        SeparatedCharSequence topicParts,
        Topic<T> topicPart,
        ARG0 arg0, ARG1 arg1, ARG2 arg2, ARG3 arg3,
        Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink,
        Consumer4<ARG0, ARG1, ARG2, ARG3> end) {
        TopicFinder.find(topicPart, topicParts, arg0, arg1, arg2, arg3, sink, end);
    }


    @SneakyThrows
    public static <T, ARG0, ARG1, ARG2, ARG3> void find(
        String topicParts,
        Topic<T> topicPart,
        ARG0 arg0, ARG1 arg1, ARG2 arg2, ARG3 arg3,
        Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink,
        Consumer4<ARG0, ARG1, ARG2, ARG3> end) {
        TopicFinder.find(topicPart, topicParts, arg0, arg1, arg2, arg3, sink, end);
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
        ConcurrentMap<String, Topic<T>> children = child;
        if (children != null) {
            for (Map.Entry<String, Topic<T>> entry : children.entrySet()) {
                Topic<T> topic = entry.getValue();
                boolean cleaned = topic.cleanup(handler);
                if (cleaned) {
                    cleaned = removeChildIfEmpty(entry.getKey(), topic);
                }
                if (handler != null) {
                    handler.accept(cleaned, topic);
                }
            }
        }

        synchronized (this) {
            ConcurrentMap<T, Integer> subscribers = this.subscribers;
            if (subscribers != null && subscribers != DETACHED && subscribers.isEmpty()) {
                this.subscribers = null;
            }
            children = child;
            if (children != null && children.isEmpty()) {
                child = null;
                starChild = null;
                doubleStarChild = null;
            }
            return (this.subscribers == null || this.subscribers == DETACHED) && child == null;
        }
    }

    private boolean removeChildIfEmpty(String key, Topic<T> topic) {
        synchronized (this) {
            ConcurrentMap<String, Topic<T>> children = child;
            if (children == null || children.get(key) != topic) {
                return false;
            }
            synchronized (topic) {
                if (!CollectionUtils.isEmpty(topic.subscribers) ||
                    !CollectionUtils.isEmpty(topic.child)) {
                    return false;
                }
                if (children.remove(key, topic)) {
                    topic.subscribers = detachedMarker();
                    topic.child = null;
                    topic.starChild = null;
                    topic.doubleStarChild = null;
                    updateWildcardChild(topic, true);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean cleanup() {
        return cleanup(null);
    }

    public void clean() {
        Collection<Topic<T>> detached = detachChildren(false);
        for (Topic<T> topic : detached) {
            topic.cleanDetached();
        }
    }

    private void cleanDetached() {
        Collection<Topic<T>> detached = detachChildren(true);
        for (Topic<T> topic : detached) {
            topic.cleanDetached();
        }
    }

    private Collection<Topic<T>> detachChildren(boolean detached) {
        synchronized (this) {
            boolean detachSelf = detached || hasDetachedAncestor();
            ConcurrentMap<String, Topic<T>> children = child;
            Collection<Topic<T>> snapshot;
            if (children == null || children.isEmpty()) {
                snapshot = Collections.emptyList();
            } else {
                snapshot = new ArrayList<>(children.values());
                // 先发布直属子节点的 detached 状态，再断开 child Map，订阅可据此重建到当前树。
                for (Topic<T> topic : snapshot) {
                    synchronized (topic) {
                        topic.subscribers = detachedMarker();
                        topic.starChild = null;
                        topic.doubleStarChild = null;
                    }
                }
            }
            subscribers = detachSelf ? detachedMarker() : null;
            child = null;
            starChild = null;
            doubleStarChild = null;
            return snapshot;
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
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        Topic<T> topic = this;
        while (topic != null) {
            hash = 31 * hash + topic.part.hashCode();
            topic = topic.parent;
        }
        return hash;
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

    public TopicView view() {
        TopicView view = new TopicView();
        view.setPart(part);

        ConcurrentMap<T, Integer> subscribers = this.subscribers;
        if (MapUtils.isNotEmpty(subscribers)) {
            view.setSubscribers(subscribers.keySet());
        }
        ConcurrentMap<String, Topic<T>> child = this.child;
        if (child != null) {
            view.setChildren(
                Collections2.
                    transform(child.values(),
                              Topic::view)
            );
        }
        return view;
    }
}
