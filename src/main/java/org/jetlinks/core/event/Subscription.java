package org.jetlinks.core.event;

import com.google.common.collect.Collections2;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.core.Routable;
import org.jetlinks.core.utils.TopicUtils;
import org.springframework.util.Assert;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
@Slf4j
public class Subscription implements Externalizable {
    private static final long serialVersionUID = -6849794470754667710L;

    public static final Feature[] DEFAULT_FEATURES = {
        Feature.local,
        Feature.broker,
        Feature.shared
    };

    //订阅者标识
    private String subscriber;

    //订阅主题,主题以/分割,如: /device/TS-01/09012/message 支持通配符 /device/**
    private String[] topics;

    //订阅特性
    private Feature[] features;

    private transient Runnable doOnSubscribe;

    private transient Consumer<TopicPayload> onDropped;

    //优先级,值越小优先级越高,优先级高的订阅者会先收到消息
    private int priority = Integer.MAX_VALUE;

    private long time;

    public Subscription() {
    }

    public Subscription(String subscriber, String[] topics, Feature[] features, Runnable doOnSubscribe, int priority) {
        this.subscriber = subscriber;
        this.topics = topics;
        this.features = features;
        this.doOnSubscribe = doOnSubscribe;
        this.priority = priority;
    }

    public void discard(TopicPayload payload) {
        if (onDropped != null) {
            onDropped.accept(payload);
        }
    }

    public void dropped(TopicPayload payload) {
        if (onDropped != null) {
            onDropped.accept(payload);
        } else {
            log.warn("eventbus buffer overflow, drop event:{},subscription:{}", payload.getTopic(), this);
        }
    }

    public Consumer<TopicPayload> getDropListener() {
        return onDropped;
    }

    public static Subscription of(String subscriber, String... topic) {

        return builder()
            .subscriberId(subscriber)
            .topics(topic)
            .build();
//        return new Subscription(subscriber, topic, DEFAULT_FEATURES, null);
    }

    public static Subscription of(String subscriber, String[] topic, Feature... features) {
        return builder()
            .subscriberId(subscriber)
            .topics(topic)
            .features(features)
            .build();
    }

    public static Subscription of(String subscriber, String topic, Feature... features) {
        return builder()
            .subscriberId(subscriber)
            .topics(topic)
            .features(features)
            .build();
        //return new Subscription(subscriber, new String[]{topic}, features, null);
    }

    public Subscription copy(Feature... newFeatures) {
        return new Subscription(subscriber, topics, newFeatures, null, null, priority, time);
    }

    public Subscription onSubscribe(Runnable sub) {
        this.doOnSubscribe = sub;
        return this;
    }

    public boolean hasFeature(Feature feature) {
        return feature.in(this.features);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(subscriber);

        out.writeInt(topics.length);
        for (String topic : topics) {
            out.writeUTF(topic);
        }

        out.writeLong(EnumDict.toMask(features));
        out.writeInt(priority);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.subscriber = in.readUTF();

        int len = in.readInt();
        topics = new String[len];
        for (int i = 0; i < len; i++) {
            topics[i] = in.readUTF();
        }

        features = EnumDict
            .getByMask(Feature.class, in.readLong())
            .toArray(new Feature[0]);
        priority = in.readInt();
    }

    @Override
    public String toString() {
        return "Subscription{" +
            "subscriber='" + subscriber + '\'' +
            ", topics=" + Arrays.toString(topics) +
            ", features=" + Arrays.toString(features) +
            '}';
    }

    @AllArgsConstructor
    @Getter
    @Dict("subscription-feature")
    public enum Feature implements EnumDict<String> {

        /**
         * 如果相同的订阅者{@link Subscription#subscriber},只有一个订阅者收到消息.
         *
         * @see Routable#routeKey()
         * @see Feature#sharedOldest
         * @see Feature#sharedLocalFirst
         */
        shared("shared"),
        //订阅本地消息
        local("订阅本地消息"),
        //订阅来自代理(集群)的消息
        broker("订阅集群消息"),

        sharedOldest("相同订阅者总是最先订阅的收到数据"),
        sharedLocalFirst("集群下相同的订阅者总是本地的优先收到数据"),
        /**
         * 集群传递时进行安全序列化,防止跨服务无法序列化事件类
         * 收到消息请使用{@link TopicPayload#decode(Class)}反序列化为目标对象
         *
         * @see org.jetlinks.core.utils.SerializeUtils#convertToSafelySerializable(Object)
         */
        safetySerialization("安全序列化"),

        /**
         * 共享订阅时,数据实现{@link Routable}时根据按{@link Routable#hash(Object...)}进行负载均衡.
         */
        sharedHashed("使用hash方式路由共享订阅"),

        /**
         * 共享订阅时,路由到最小负载的订阅者.
         */
        sharedMinimumLoad("使用最小负载方式路由共享订阅");

        private final String text;

        //订阅本地和集群的消息
        public static final Feature[] clusterFeatures = {
            Feature.local,
            Feature.broker
        };
        //共享订阅本地和集群的消息.
        public static final Feature[] clusterSharedFeatures = {
            Feature.local,
            Feature.broker,
            Feature.shared
        };
        //共享订阅本地和集群的消息,优先本地订阅者收到消息
        public static final Feature[] clusterSharedLocalFirstFeatures = {
            Feature.local,
            Feature.broker,
            Feature.shared,
            Feature.sharedLocalFirst
        };
        //共享订阅本地和集群的消息,以hash方式负载均衡
        public static final Feature[] clusterSharedHashFeatures = {
            Feature.local,
            Feature.broker,
            Feature.shared,
            Feature.sharedHashed
        };
        //共享订阅本地和集群的消息,以最小负载方式负载均衡
        public static final Feature[] clusterSharedMinimumLoadFeatures = {
            Feature.local,
            Feature.broker,
            Feature.shared,
            Feature.sharedMinimumLoad
        };
        //共享订阅本地和集群的消息,最先订阅的订阅者收到消息
        public static final Feature[] clusterSharedOldestFeatures = {
            Feature.local,
            Feature.broker,
            Feature.shared,
            Feature.sharedOldest
        };

        @Override
        public String getValue() {
            return name();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        //订阅者标识
        private String subscriber;

        //订阅主题,主题以/分割,如: /device/TS-01/09012/message 支持通配符 /device/**
        private final Set<String> topics = new HashSet<>();

        //订阅特性
        private final Set<Feature> features = new HashSet<>();

        private Runnable doOnSubscribe;
        private Consumer<TopicPayload> onDropped;
        private int priority;
        private long time;

        public Builder randomSubscriberId() {
            return subscriberId(UUID.randomUUID().toString());
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder subscriberId(String id) {
            this.subscriber = id;
            return this;
        }

        public Builder subscriberId(CharSequence id) {
            this.subscriber = String.valueOf(id);
            return this;
        }

        public Builder topics(CharSequence... topics) {
            if (topics.length == 1) {
                this.topics.addAll(TopicUtils.expand(topics[0].toString()));
                return this;
            }
            return topics(Collections2.transform(Arrays.asList(topics), String::valueOf));
        }

        public Builder topics(String... topics) {
            if (topics.length == 1) {
                this.topics.addAll(TopicUtils.expand(topics[0]));
                return this;
            }
            return topics(Arrays.asList(topics));
        }

        public Builder topics(Collection<String> topics) {
            this.topics.addAll(topics.stream()
                                     .flatMap(topic -> TopicUtils.expand(topic).stream())
                                     .collect(Collectors.toSet()));
            return this;
        }

        public Builder features(Feature... features) {
            this.features.addAll(Arrays.asList(features));
            return this;
        }

        public Builder doOnSubscribe(Runnable runnable) {
            this.doOnSubscribe = runnable;
            return this;
        }

        public Builder justLocal() {
            this.features.clear();
            return features(Feature.local);
        }

        public Builder justBroker() {
            this.features.clear();
            return features(Feature.broker);
        }

        public Builder onDropped(Consumer<TopicPayload> consumer) {
            if (onDropped == null || onDropped == consumer) {
                onDropped = consumer;
            } else {
                onDropped = onDropped.andThen(consumer);
            }
            return this;
        }

        public Builder local() {
            return features(Feature.local);
        }

        public Builder broker() {
            return features(Feature.broker);
        }

        public Builder shared() {
            return features(Feature.shared);
        }

        public Builder sharedOldest() {
            return features(Feature.shared, Feature.sharedOldest);
        }

        public Builder sharedLocalFirst() {
            return features(Feature.shared, Feature.sharedLocalFirst);
        }

        public Builder time(long time) {
            this.time = time;
            return this;
        }

        public Subscription build() {
            if (features.isEmpty()) {
                local();
            }
            Assert.notEmpty(topics, "topic cannot be empty");
            Assert.hasText(subscriber, "subscriber cannot be empty");
            return new Subscription(
                subscriber,
                topics.toArray(new String[0]),
                features.toArray(new Feature[0]),
                doOnSubscribe,
                onDropped,
                priority,
                time);
        }

    }
}
