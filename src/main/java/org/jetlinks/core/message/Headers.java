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
     * 在保持在线时,忽略连接状态信息,设备是否在线以: {@link Headers#keepOnlineTimeoutSeconds}指定为准
     */
    HeaderKey<Boolean> keepOnlineIgnoreConnection = HeaderKey.of("keepOnlineIC", false, Boolean.class);

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
    @Deprecated
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


    /**
     * 上报属性中是否包含geo信息,如果设置为false,上报属性时则不处理地理位置相关逻辑,可能提高一些性能
     *
     * @see org.jetlinks.core.message.property.ReportPropertyMessage
     */
    HeaderKey<Boolean> propertyContainsGeo = HeaderKey.of("containsGeo", true);

    /**
     * 明确定义上报属性中包含的geo属性字段,在设备物模型属性数量较大时有助于提升地理位置信息处理性能
     *
     * @see org.jetlinks.core.message.property.ReportPropertyMessage
     */
    HeaderKey<String> geoProperty = HeaderKey.of("geoProperty", null, String.class);

    /**
     * 在设备离线时,标记是否清理所有会话.
     * <p>
     * 通常用于短连接方式接入平台的场景,
     * 在集群的多台节点中存在同一个设备的会话时,默认只有集群全部会话失效时,设备才算离线.
     * 可通过在发送离线消息中指定header: clearAllSession来标识是否让集群全部会话都失效.
     *
     * <pre>{@code
     *     message.addHeader(Headers.clearAllSession,true);
     * }</pre>
     *
     * @see DeviceOfflineMessage
     */
    HeaderKey<Boolean> clearAllSession = HeaderKey.of("clearAllSession", false, Boolean.class);

//    /**
//     * 当返回错误{@link ThingMessageReply#isSuccess()}false时,是否通过抛出异常的方式返回错误信息.默认为true.
//     *
//     * @see ThingMessageReply#isSuccess()
//     * @see ThingMessageReply#getCode()
//     * @see org.jetlinks.core.exception.DeviceOperationException
//     * @see org.jetlinks.core.enums.ErrorCode
//     */
//    HeaderKey<Boolean> throwWhenReplyError = HeaderKey.of("throwWhenReplyError", false, Boolean.class);

    /**
     * 消息是否支持来自多个接入网关,某些网关会过滤掉不属于自己网关的数据,设置此header为true以忽略过滤.
     */
    HeaderKey<Boolean> multiGateway = HeaderKey.of("multiGateway", false, Boolean.class);

    /**
     * copy有意义的header到新到消息中,比如标记异步,超时等信息
     *
     * @param from from
     * @param to   to
     */
    static void copyFunctionalHeader(Message from, Message to) {

        from.getHeader(async).ifPresent(val -> to.addHeader(async, val));
        from.getHeader(timeout).ifPresent(val -> to.addHeader(timeout, val));
        from.getHeader(sendAndForget).ifPresent(val -> to.addHeader(sendAndForget, val));
        from.getHeader(force).ifPresent(val -> to.addHeader(force, val));
        from.getHeader(ignore).ifPresent(val -> to.addHeader(ignore, val));
        from.getHeader(ignoreLog).ifPresent(val -> to.addHeader(ignoreLog, val));

    }
}
