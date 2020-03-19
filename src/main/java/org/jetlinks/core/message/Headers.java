package org.jetlinks.core.message;

import java.util.concurrent.TimeUnit;

public interface Headers {

    /**
     * 默认只有{@link DeviceMessageReply} 才会处理回复逻辑,此标记将强制进行回复
     *
     * @see Message#getMessageId()
     */
    HeaderKey<Boolean> forceReply = HeaderKey.of("forceReply", true);

    /**
     * 异步消息,当发往设备的消息标记了为异步时,设备网关服务发送消息到设备后将立即回复{@link org.jetlinks.core.enums.ErrorCode#REQUEST_HANDLING}到发送端
     *
     * @see org.jetlinks.core.enums.ErrorCode#REQUEST_HANDLING
     */
    HeaderKey<Boolean> async = HeaderKey.of("async", false);

    /**
     * 发送既不管
     */
    HeaderKey<Boolean> sendAndForget = HeaderKey.of("sendAndForget", false);

    /**
     * 指定发送消息的超时时间
     */
    HeaderKey<Long> timeout = HeaderKey.of("timeout", TimeUnit.SECONDS.toMillis(10));

    //******** 分片消息,一个请求,设备将结果分片返回,通常用于处理大消息. **********
    //分片消息ID(为平台下发消息时的消息ID)
    HeaderKey<String> fragmentBodyMessageId = HeaderKey.of("frag_msg_id", null);
    //分片数量
    HeaderKey<Integer> fragmentNumber = HeaderKey.of("frag_num", 0);

    //当前分片
    HeaderKey<Integer> fragmentPart = HeaderKey.of("frag_part", 0);

    //集群间消息传递标记
    HeaderKey<String> sendFrom = HeaderKey.of("send-from", null);
    HeaderKey<String> replyFrom = HeaderKey.of("reply-from", null);

    //设备上报属性信息
    /**
     * @see org.jetlinks.core.message.property.ReportPropertyMessage
     */
    @Deprecated
    HeaderKey<Boolean> reportProperties = HeaderKey.of("report-properties", false);

    //上报派生属性
    @Deprecated
    HeaderKey<Boolean> reportDerivedMetadata = HeaderKey.of("derived-metadata", false);
}
