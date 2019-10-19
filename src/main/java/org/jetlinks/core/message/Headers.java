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

    //******** 分片**********
    HeaderKey<Boolean> sharding = HeaderKey.of("sharding", false);

    HeaderKey<String> partMessageId = HeaderKey.of("part_msg_id", null);

    HeaderKey<Integer> shardingPart = HeaderKey.of("sharding_parts_of", 0);

    HeaderKey<Integer> shardingPartTotal = HeaderKey.of("sharding_parts_total", 0);


}
