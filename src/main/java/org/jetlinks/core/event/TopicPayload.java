package org.jetlinks.core.event;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Decoder;
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
    public TopicPayload slice() {
        return TopicPayload.of(topic, payload.slice());
    }

    @Override
    public void release() {
        payload.release();
    }

    @Override
    public void release(int dec) {
        payload.release(dec);
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

    @Override
    public JSONObject bodyToJson(boolean release) {
        return payload.bodyToJson(release);
    }

    @Override
    public JSONArray bodyToJsonArray(boolean release) {
        return payload.bodyToJsonArray(release);
    }

    @Override
    public String bodyToString() {
        return payload.bodyToString();
    }

    @Override
    public String bodyToString(boolean release) {
        return payload.bodyToString(release);
    }

    @Override
    public Object decode() {
        return payload.decode();
    }

    @Override
    public Object decode(boolean release) {
        return payload.decode(release);
    }

    @Override
    public <T> T decode(Class<T> decoder) {
        return payload.decode(decoder);
    }

    @Override
    public <T> T decode(Class<T> decoder, boolean release) {
        return payload.decode(decoder, release);
    }

    @Override
    public <T> T decode(Decoder<T> decoder) {
        return payload.decode(decoder);
    }

    @Override
    public <T> T decode(Decoder<T> decoder, boolean release) {
        return payload.decode(decoder, release);
    }

    public Map<String, String> getTopicVars(String pattern) {
        return TopicUtils.getPathVariables(pattern, getTopic());
    }
}
