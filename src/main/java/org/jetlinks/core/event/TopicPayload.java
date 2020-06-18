package org.jetlinks.core.event;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetlinks.core.Payload;

import javax.annotation.Nonnull;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class TopicPayload implements Payload {

    private String topic;

    private Payload payload;

    @Nonnull
    @Override
    public ByteBuf getBody() {
        return payload.getBody();
    }
}
