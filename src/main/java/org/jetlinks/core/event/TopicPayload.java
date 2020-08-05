package org.jetlinks.core.event;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetlinks.core.Payload;
import org.jetlinks.core.utils.TopicUtils;

import javax.annotation.Nonnull;
import java.util.Map;

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

    @Override
    public void release() {
        payload.release();
    }

    @Override
    public void release(int dec) {
        payload.release();
    }

    @Override
    public void retain() {
        payload.retain();
    }

    @Override
    public void retain(int inc) {
        payload.retain(inc);
    }

    @Override
    public String toString() {
        return "{" +
                "topic='" + topic + '\'' +
                ", payload=" + payload +
                '}';
    }

    public Map<String, String> getTopicVars(String pattern) {
        return TopicUtils.getPathVariables(pattern, getTopic());
    }
}
