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

    public static final Feature[] DEFAULT_FEATURES = {Feature.local};

    //订阅者标识
    private final String subscriber;

    //订阅主题,主题以/分割,如: /device/TS-01/09012/message 支持通配符 /device/**
    private final String[] topics;

    //订阅特性
    private final Feature[] features;

    public static Subscription of(String subscriber, String[] topic) {
        return new Subscription(subscriber, topic, DEFAULT_FEATURES);
    }

    public static Subscription of(String subscriber, String[] topic, Feature... features) {
        return new Subscription(subscriber, topic, features);
    }

    public Subscription copy(Feature... newFeatures) {
        return new Subscription(subscriber, topics, newFeatures);
    }

    public boolean hasFeature(Subscription.Feature feature) {
        return feature.in(this.features);
    }

    @AllArgsConstructor
    @Getter
    @Dict("subscription-feature")
    public enum Feature implements EnumDict<String> {

        //如果相同的订阅者,只有一个订阅者收到消息
        atMostOnce("at most once"),
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
