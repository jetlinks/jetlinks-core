package org.jetlinks.core.collector;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;
import org.jetlinks.core.message.HeaderKey;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.metadata.Feature;

public interface CollectorConstants {

    interface Headers {

        HeaderKey<String> pointId = HeaderKey.of("pointId", null, String.class);

    }

    @AllArgsConstructor
    @Getter
    enum CollectorFeatures implements Feature {
        subscribable("可订阅点位数据"),
        batchSupport("支持批量采集");
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
        shutdown("已关闭"),
        connectionClosed("连接已断开");

        private final String text;

        @Override
        public String getValue() {
            return name();
        }

    }


    @AllArgsConstructor
    @Getter
    enum Codes {
        // 成功
        success(0),

        channelError(10000),
        // 通信错误
        channelCommunicationError(10001),
        // 配置错误
        channelConfigError(10002),

        collectorError(20000),
        // 通信错误
        collectorCommunicationError(20001),
        // 配置错误
        collectorConfigError(20002),

        pointError(30000),
        // 配置错误
        pointConfigError(30001),
        // 编解码错误
        pointCodecError(30002),
        // 不支持读
        pointUnsupportedRead(3004),
        // 不支持写
        pointUnsupportedWrite(3005),

        ;

        final int code;

    }


}
