package org.jetlinks.core.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class Subscription implements Serializable {
    private static final long serialVersionUID = -6849794470754667710L;

    public static final Feature[] DEFAULT_FEATURES = Subscription.Feature.values();

    //订阅者标识
    private final String subscriber;

    //订阅主题,主题以/分割,如: /device/TS-01/09012/message 支持通配符 /device/**
    private final String[] topics;

    //订阅特性
    private final Feature[] features;

    private Runnable doOnSubscribe;

    public static Subscription of(String subscriber, String... topic) {
        return new Subscription(subscriber, topic, DEFAULT_FEATURES, null);
    }

    public static Subscription of(String subscriber, String[] topic, Feature... features) {
        return new Subscription(subscriber, topic, features, null);
    }

    public static Subscription of(String subscriber, String topic, Feature... features) {
        return new Subscription(subscriber, new String[]{topic}, features, null);
    }

    public Subscription copy(Feature... newFeatures) {
        return new Subscription(subscriber, topics, newFeatures, null);
    }

    public Subscription onSubscribe(Runnable sub) {
        this.doOnSubscribe = sub;
        return this;
    }

    public boolean hasFeature(Subscription.Feature feature) {
        return feature.in(this.features);
    }

    @AllArgsConstructor
    @Getter
    @Dict("subscription-feature")
    public enum Feature implements EnumDict<String> {

        //如果相同的订阅者,只有一个订阅者收到消息
        shared("shared"),
        //订阅本地消息
        local("订阅本地消息"),
        //订阅来自代理的消息
        broker("订阅代理消息");

        private final String text;

        @Override
        public String getValue() {
            return name();
        }
    }
}
