package org.jetlinks.core.message;

import java.util.concurrent.TimeUnit;

public interface Headers {

    /**
     * 强制执行
     */
    HeaderKey<Boolean> force = HeaderKey.of("force", true);

    /**
     * 保持在线,与{@link DeviceOnlineMessage}配合使用.
     * @see Headers#keepOnlineTimeoutSeconds
     */
    HeaderKey<Boolean> keepOnline = HeaderKey.of("keepOnline", true);

    /**
     * 保持在线超时时间,超过指定时间未收到消息则认为离线
     */
    HeaderKey<Integer> keepOnlineTimeoutSeconds = HeaderKey.of("keepOnlineTimeoutSeconds", 600);

    /**
     * 异步消息,当发往设备的消息标记了为异步时,设备网关服务发送消息到设备后将立即回复{@link org.jetlinks.core.enums.ErrorCode#REQUEST_HANDLING}到发送端
     *
     * @see org.jetlinks.core.enums.ErrorCode#REQUEST_HANDLING
     */
    HeaderKey<Boolean> async = HeaderKey.of("async", false);

    /**
     * 客户端地址,通常为设备IP地址
     */
    HeaderKey<String> clientAddress = HeaderKey.of("cliAddr", "/");

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

    //是否为最后一个分配,如果分片数量不确定则使用这个来表示分片结束了.
    HeaderKey<Boolean> fragmentLast = HeaderKey.of("frag_last", false);

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
