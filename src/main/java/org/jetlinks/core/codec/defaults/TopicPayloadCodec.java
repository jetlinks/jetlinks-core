package org.jetlinks.core.codec.defaults;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.utils.BytesUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TopicPayloadCodec implements Codec<TopicPayload> {

    public static final TopicPayloadCodec INSTANCE = new TopicPayloadCodec();

    @Override
    public Class<TopicPayload> forType() {
        return TopicPayload.class;
    }

    @Nullable
    @Override
    public TopicPayload decode(@Nonnull Payload payload) {

        ByteBuf byteBuf = payload.getBody();

        byte[] topicLen = new byte[4];

        byteBuf.getBytes(0, topicLen);
        int bytes = BytesUtils.beToInt(topicLen);

        byte[] topicBytes = new byte[bytes];

        byteBuf.getBytes(4, topicBytes);
        String topic = new String(topicBytes);

        int idx = 4 + bytes;

        ByteBuf body = byteBuf.slice(idx, byteBuf.readableBytes() - idx);
        byteBuf.resetReaderIndex();
        return TopicPayload.of(topic, Payload.of(body));
    }

    @Override
    public Payload encode(TopicPayload body) {

        byte[] topic = body.getTopic().getBytes();
        byte[] topicLen = BytesUtils.intToBe(topic.length);

        return Payload.of(ByteBufAllocator.DEFAULT.compositeBuffer(3)
                .addComponent(true, Unpooled.wrappedBuffer(topicLen))
                .addComponent(true, Unpooled.wrappedBuffer(topic))
                .addComponent(true, body.getBody()));

//        return Payload.of(Unpooled.buffer()
//                .writeBytes(topicLen)
//                .writeBytes(topic)
//                .writeBytes(body.getBytes())
//        );

    }
}
