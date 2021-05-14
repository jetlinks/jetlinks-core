package org.jetlinks.core.message;

import java.util.concurrent.TimeUnit;

public interface Headers {

    /**
     * 强制执行
     */
    HeaderKey<Boolean> force = HeaderKey.of("force", true, Boolean.class);

    /**
     * 保持在线,与{@link DeviceOnlineMessage}配合使用.
     *
     * @see Headers#keepOnlineTimeoutSeconds
     */
    HeaderKey<Boolean> keepOnline = HeaderKey.of("keepOnline", true, Boolean.class);

    /**
     * 保持在线超时时间,超过指定时间未收到消息则认为离线
     */
    HeaderKey<Integer> keepOnlineTimeoutSeconds = HeaderKey.of("keepOnlineTimeoutSeconds", 600, Integer.class);

    /**
     * 异步消息,当发往设备的消息标记了为异步时,设备网关服务发送消息到设备后将立即回复{@link org.jetlinks.core.enums.ErrorCode#REQUEST_HANDLING}到发送端
     *
     * @see org.jetlinks.core.enums.ErrorCode#REQUEST_HANDLING
     */
    HeaderKey<Boolean> async = HeaderKey.of("async", false, Boolean.class);

    /**
     * 客户端地址,通常为设备IP地址
     */
    HeaderKey<String> clientAddress = HeaderKey.of("cliAddr", "/", String.class);

    /**
     * 发送既不管
     */
    HeaderKey<Boolean> sendAndForget = HeaderKey.of("sendAndForget", false);

    /**
     * 指定发送消息的超时时间
     */
    HeaderKey<Long> timeout = HeaderKey.of("timeout", TimeUnit.SECONDS.toMillis(10), Long.class);

    /**
     * 是否合并历史属性数据,设置此消息头后,将会把历史最新的消息合并到消息体里
     *
     * @see org.jetlinks.core.message.property.ReportPropertyMessage
     * @see org.jetlinks.core.message.property.ReadPropertyMessageReply
     * @see org.jetlinks.core.message.property.WritePropertyMessageReply
     * @since 1.1.4
     */
    HeaderKey<Boolean> mergeLatest = HeaderKey.of("mergeLatest", false, Boolean.class);

    /**
     * 是否为转发到父设备的消息
     *
     * @since 1.1.6
     */
    HeaderKey<Boolean> dispatchToParent = HeaderKey.of("dispatchToParent", false, Boolean.class);

    //******** 分片消息,一个请求,设备将结果分片返回,通常用于处理大消息. **********
    //分片消息ID(为平台下发消息时的消息ID)
    HeaderKey<String> fragmentBodyMessageId = HeaderKey.of("frag_msg_id", null, String.class);
    //分片数量
    HeaderKey<Integer> fragmentNumber = HeaderKey.of("frag_num", 0, Integer.class);

    //是否为最后一个分配,如果分片数量不确定则使用这个来表示分片结束了.
    HeaderKey<Boolean> fragmentLast = HeaderKey.of("frag_last", false, Boolean.class);

    //当前分片
    HeaderKey<Integer> fragmentPart = HeaderKey.of("frag_part", 0, Integer.class);

    //集群间消息传递标记
    HeaderKey<String> sendFrom = HeaderKey.of("send-from", null, String.class);
    HeaderKey<String> replyFrom = HeaderKey.of("reply-from", null, String.class);

    //是否使用时间戳作为数据ID
    HeaderKey<Boolean> useTimestampAsId = HeaderKey.of("useTimestampId", false, Boolean.class);

    //是否属性为部分属性,如果为true,在列式存储策略下,将会把之前上报的属性合并到一起进行存储.
    HeaderKey<Boolean> partialProperties = HeaderKey.of("partialProperties", false, Boolean.class);

    /**
     * 是否开启追踪,开启后header中将添加各个操作的时间戳
     *
     * @see org.jetlinks.core.utils.DeviceMessageTracer
     */
    HeaderKey<Boolean> enableTrace = HeaderKey.of("_trace", Boolean.getBoolean("device.message.trace.enabled"), Boolean.class);

    /**
     * 标记数据不存储
     *
     * @see org.jetlinks.core.message.property.ReadPropertyMessage
     * @see org.jetlinks.core.message.property.ReadPropertyMessageReply
     * @see org.jetlinks.core.message.property.WritePropertyMessageReply
     * @since 1.1.6
     */
    HeaderKey<Boolean> ignoreStorage = HeaderKey.of("ignoreStorage", false, Boolean.class);

    /**
     * 忽略记录日志
     */
    HeaderKey<Boolean> ignoreLog = HeaderKey.of("ignoreLog", false, Boolean.class);

    /**
     * 忽略某些操作,具体由不同的消息决定
     */
    HeaderKey<Boolean> ignore = HeaderKey.of("ignore", false, Boolean.class);

    /**
     * 忽略会话创建,如果设备未在线,默认为创建会话,设置此header为true后则不会自动创建会话.
     */
    HeaderKey<Boolean> ignoreSession = HeaderKey.of("ignoreSession", false, Boolean.class);

    /**
     * 产品ID
     */
    HeaderKey<String> productId = HeaderKey.of("productId", null, String.class);
}
