package org.jetlinks.core.message;

import java.util.concurrent.TimeUnit;

public interface Headers {

    /**
     * 强制回复消息
     */
    HeaderKey<Boolean> forceReply = HeaderKey.of("force-reply", true);

    HeaderKey<Boolean> asyncSupport = HeaderKey.of("async-support", false);

    HeaderKey<Boolean> async = HeaderKey.of("async", false);

    HeaderKey<Long> timeout = HeaderKey.of("timeout", TimeUnit.SECONDS.toMillis(10));

    //******** 分片 **********
    //分片消息ID
    HeaderKey<String> fragmentBodyMessageId = HeaderKey.of("frag_msg_id", null);
    //当前分片
    HeaderKey<Integer> fragmentPart = HeaderKey.of("frg_part", 0);
    //分片数量
    HeaderKey<Integer> fragmentNumber = HeaderKey.of("frg_num", 0);


}
