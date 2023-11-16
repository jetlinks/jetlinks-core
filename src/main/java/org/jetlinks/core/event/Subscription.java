package org.jetlinks.core.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.core.utils.TopicUtils;
import org.springframework.util.Assert;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public class Subscription implements Externalizable {
    private static final long serialVersionUID = -6849794470754667710L;

    public static final Feature[] DEFAULT_FEATURES = Subscription.Feature.values();

    //订阅者标识
    private String subscriber;

    //订阅主题,主题以/分割,如: /device/TS-01/09012/message 支持通配符 /device/**
    private String[] topics;

    //订阅特性
    private Feature[] features;

    private transient Runnable doOnSubscribe;

    //优先级,值越小优先级越高,优先级高的订阅者会先收到消息
    private int priority = Integer.MAX_VALUE;

    public Subscription() {
    }

    public static Subscription of(String subscriber, String... topic) {

        return Subscription
            .builder()
            .subscriberId(subscriber)
            .topics(topic)
            .build();
//        return new Subscription(subscriber, topic, DEFAULT_FEATURES, null);
    }

    public static Subscription of(String subscriber, String[] topic, Feature... features) {
        return Subscription
            .builder()
            .subscriberId(subscriber)
            .topics(topic)
            .features(features)
            .build();
    }

    public static Subscription of(String subscriber, String topic, Feature... features) {
        return Subscription
            .builder()
            .subscriberId(subscriber)
            .topics(topic)
            .features(features)
            .build();
        //return new Subscription(subscriber, new String[]{topic}, features, null);
    }

    public Subscription copy(Feature... newFeatures) {
        return new Subscription(subscriber, topics, newFeatures, null, priority);
    }

    public Subscription onSubscribe(Runnable sub) {
        this.doOnSubscribe = sub;
        return this;
    }

    public boolean hasFeature(Subscription.Feature feature) {
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

        //如果相同的订阅者,只有一个订阅者收到消息
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
        safetySerialization("安全序列化");

        private final String text;

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
        private int priority;

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

        public Builder topics(String... topics) {
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

        public Subscription build() {
            if (features.isEmpty()) {
                local();
            }
            Assert.notEmpty(topics, "topic cannot be empty");
            Assert.hasText(subscriber, "subscriber cannot be empty");
            return new Subscription(subscriber, topics.toArray(new String[0]), features.toArray(new Feature[0]), doOnSubscribe, priority);
        }

    }
}
