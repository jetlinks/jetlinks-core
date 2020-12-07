package org.jetlinks.core.codec.defaults;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.utils.BytesUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Slf4j
public class TopicPayloadCodec implements Codec<TopicPayload> {

    public static final TopicPayloadCodec INSTANCE = new TopicPayloadCodec();

    @Override
    public Class<TopicPayload> forType() {
        return TopicPayload.class;
    }


    public static TopicPayload doDecode(ByteBuf byteBuf){
        byte[] topicLen = new byte[4];

        byteBuf.getBytes(0, topicLen);
        int bytes = BytesUtils.beToInt(topicLen);

        byte[] topicBytes = new byte[bytes];

        byteBuf.getBytes(4, topicBytes);
        String topic = new String(topicBytes);

        int idx = 4 + bytes;

        ByteBuf body = byteBuf.slice(idx, byteBuf.writerIndex() - idx);
        byteBuf.resetReaderIndex();
        return TopicPayload.of(topic, Payload.of(body));
    }

    public static ByteBuf doEncode(TopicPayload body){
        byte[] topic = body.getTopic().getBytes();
        byte[] topicLen = BytesUtils.intToBe(topic.length);
        try {
            ByteBuf bodyBuf = body.getBody();
            return ByteBufAllocator.DEFAULT
                    .buffer(topicLen.length + topic.length + bodyBuf.writerIndex())
                    .writeBytes(topicLen)
                    .writeBytes(topic)
                    .writeBytes(bodyBuf, 0, bodyBuf.writerIndex());
        } catch (Throwable e) {
            log.error("encode topic [{}] payload error", body.getTopic());
            throw e;
        }
    }

    @Nullable
    @Override
    public TopicPayload decode(@Nonnull Payload payload) {
        return doDecode(payload.getBody());
    }

    @Override
    public Payload encode(TopicPayload body) {

        byte[] topic = body.getTopic().getBytes();
        byte[] topicLen = BytesUtils.intToBe(topic.length);
        try {
            ByteBuf bodyBuf = body.getBody();
            return Payload.of(ByteBufAllocator.DEFAULT
                                      .buffer(topicLen.length + topic.length + bodyBuf.writerIndex())
                                      .writeBytes(topicLen)
                                      .writeBytes(topic)
                                      .writeBytes(bodyBuf, 0, bodyBuf.writerIndex()));
        } catch (Throwable e) {
            log.error("encode topic [{}] payload error", body.getTopic());
            throw e;
        }

//        return Payload.of(Unpooled.buffer()
//                .writeBytes(topicLen)
//                .writeBytes(topic)
//                .writeBytes(body.getBytes())
//        );

    }
}
