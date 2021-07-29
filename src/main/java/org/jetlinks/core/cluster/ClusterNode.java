package org.jetlinks.core.cluster;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface ClusterNode {

    String getId();

    String getName();



    Map<String, Object> metadata();

    //向节点地址发送数据
    Mono<Void> send(String address, Publisher<ByteBuf> payload);

    enum State {
        online,

    }

}
