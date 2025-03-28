package org.jetlinks.core.collector;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;
import org.jetlinks.core.metadata.Feature;

public interface CollectorConstants {


    @AllArgsConstructor
    @Getter
    enum CollectorFeatures implements Feature {
        subscribable("可订阅点位数据"),

        ;
        final String name;

        @Override
        public final String getId() {
            return name();
        }

        @Override
        public final String getType() {
            return "CollectorFeature";
        }
    }

    @AllArgsConstructor
    @Getter
    enum States implements DataCollectorProvider.State, I18nEnumDict<String> {

        running("运行中"),
        paused("已暂停"),
        starting("启动中"),
        stopped("已停止"),
        shutdown("已关闭");

        private final String text;

        @Override
        public String getValue() {
            return name();
        }

    }


    @AllArgsConstructor
    @Getter
    enum Codes {

        success(0),

        channelError(10000),
        channelNetworkError(10001),
        channelConfigError(10002),

        collectorError(20000),
        collectorNetworkError(20001),
        collectorConfigError(20002),

        pointError(30000),
        pointConfigError(30001),
        pointCodecError(30002),
        pointUnsupportedRead(3004),
        pointUnsupportedWrite(3005),

        ;

        final int code;

    }


}
