package org.jetlinks.core.event;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.NativePayload;
import org.jetlinks.core.Payload;
import org.jetlinks.core.Routable;
import org.jetlinks.core.codec.Decoder;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.utils.TopicUtils;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@AllArgsConstructor(staticName = "of")
@Slf4j
public class TopicPayload implements Payload, Routable {

    private CharSequence topic;

    private Payload payload;

    private Map<String, Object> headers;

    public String getTopic() {
        return topic.toString();
    }

    public CharSequence getTopic0() {
        return topic;
    }

    public static TopicPayload of(CharSequence topic, Payload payload) {
        return TopicPayload.of(topic, payload, null);
    }

    public static TopicPayload of(String topic, Payload payload) {
        return TopicPayload.of(topic, payload, null);
    }

    private Map<String, Object> getOrCreateHeader() {
        return headers != null ? headers : (headers = new ConcurrentHashMap<>());
    }

    public Map<String, Object> writableHeaders() {
        return getOrCreateHeader();
    }

    public TopicPayload addHeader(String key, Object value) {
        if (key == null || value == null) {
            return this;
        }
        getOrCreateHeader().put(key, value);
        return this;
    }

    public TopicPayload addHeader(Map<String, ?> headers) {
        getOrCreateHeader().putAll(headers);
        return this;
    }

    public Object getHeader(String key) {
        return headers == null ? null : headers.get(key);
    }

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
    public boolean release() {
        return true;
    }

    @Override
    public boolean release(int dec) {
        return true;
    }

    protected boolean handleRelease(boolean success) {

        return success;
    }

    protected void deallocate() {

    }

    @Override
    public TopicPayload retain() {

        return this;
    }

    @Override
    public TopicPayload retain(int inc) {

        return this;
    }

    @Override
    public TopicPayload touch(Object o) {

        return this;
    }

    @Override
    public TopicPayload touch() {

        return this;
    }

    @Override
    public int refCnt() {
        return 0;
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

    @Override
    @SuppressWarnings("all")
    public long hash(Object... objects) {
        if (payload instanceof NativePayload) {
            if (((NativePayload<?>) payload).getNativeObject() instanceof Routable) {
                return ((Routable) ((NativePayload<?>) payload).getNativeObject()).hash(objects);
            }
        }
        return Routable.super.hash(objects);
    }

    @SuppressWarnings("all")
    public void copyRouteKeyToHeader() {
        if (payload instanceof NativePayload) {
            if (((NativePayload<?>) payload).getNativeObject() instanceof Routable) {
                addHeader(Headers.routeKey.getKey(), ((Routable) ((NativePayload<?>) payload).getNativeObject()).routeKey());
            }
        }
    }

    @SuppressWarnings("all")
    @Override
    public Object routeKey() {
        if (payload instanceof NativePayload) {
            if (((NativePayload<?>) payload).getNativeObject() instanceof Routable) {
                return ((Routable) ((NativePayload<?>) payload).getNativeObject()).routeKey();
            }
        }
        return headers.get(Headers.routeKey.getKey());
    }
}
