package org.jetlinks.core.event;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.util.ReferenceCountUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.Payload;
import org.jetlinks.core.Routable;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.core.utils.TopicUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Slf4j
public class TopicPayload implements Routable, Externalizable {

    private CharSequence topic;

    private Object payload;

    private Map<String, Object> headers;

    // Topic may be a lazily concatenated path. Cache only its materialized view and keep
    // the original sequence available to consumers that can operate on path segments.
    @Getter(AccessLevel.NONE)
    private transient volatile String topicText;

    private TopicPayload(CharSequence topic,
                         Object payload,
                         Map<String, Object> headers) {
        this.topic = topic;
        this.payload = payload;
        this.headers = headers;
    }

    public TopicPayload(){}

    public String getTopic() {
        String value = topicText;
        if (value == null) {
            value = topic.toString();
            topicText = value;
        }
        return value;
    }

    public CharSequence getTopic0() {
        return topic;
    }

    public static TopicPayload of(CharSequence topic, Object payload) {
        return TopicPayload.of(topic, payload, null);
    }

    public static TopicPayload of(CharSequence topic,
                                  Object payload,
                                  Map<String, Object> headers) {
        return new TopicPayload(topic, payload, headers);
    }

    @Deprecated
    public static TopicPayload of(CharSequence topic, Payload payload) {
        return TopicPayload.of(topic, payload, null);
    }

    @Deprecated
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

    public Object removeHeader(String key) {
        return headers == null ? null : headers.remove(key);
    }

    @Deprecated
    public boolean release() {
        return true;
    }

    @Override
    public String toString() {
        return "{" +
            "topic='" + topic + '\'' +
            ", payload=" + payload +
            '}';
    }

    public JSONObject bodyToJson() {
        return bodyToJson(true);
    }

    @SuppressWarnings("all")
    public JSONObject bodyToJson(boolean release) {
        try {
            if (payload == null) {
                return new JSONObject();
            }
            if (payload instanceof Jsonable) {
                return ((Jsonable) payload).toJson();
            }
            if (payload instanceof JSONObject) {
                return ((JSONObject) payload);
            }
            if (payload instanceof Map) {
                return new JSONObject(((Map) payload));
            }
            return FastBeanCopier.copy(payload, JSONObject::new);
        } finally {
            if (release) {
                ReferenceCountUtil.safeRelease(this);
            }
        }
    }


    @SuppressWarnings("all")
    public JSONArray bodyToJsonArray(boolean release) {
        try {
            if (payload == null) {
                return new JSONArray();
            }
            if (payload instanceof JSONArray) {
                return ((JSONArray) payload);
            }
            List<Object> collection;
            if (payload instanceof List) {
                collection = ((List<Object>) payload);
            } else if (payload instanceof Collection) {
                collection = new ArrayList<>(((Collection<Object>) payload));
            } else if (payload instanceof Object[]) {
                collection = Arrays.asList(((Object[]) payload));
            } else {
                throw new UnsupportedOperationException("body is not array");
            }
            return new JSONArray(collection);
        } finally {
            if (release) {
                ReferenceCountUtil.safeRelease(this);
            }
        }
    }

    public String bodyToString() {
        return String.valueOf(payload);
    }

    public String bodyToString(boolean release) {
        return bodyToString();
    }

    public Object decode() {
        return payload;
    }


    public Object decode(boolean release) {
        return decode();
    }


    @SuppressWarnings("all")
    public <T> T decode(Class<T> type) {
        if (type.isInstance(payload)) {
            return (T) payload;
        }
        if (type == JSONObject.class || type == Map.class) {
            return (T) bodyToJson();
        }
        if (Map.class.isAssignableFrom(type)) {
            return bodyToJson().toJavaObject(type);
        }
        return FastBeanCopier.copy(payload, type);
    }

    public <T> T decode(Class<T> decoder, boolean release) {
        return decode(decoder);
    }


    public Map<String, String> getTopicVars(String pattern) {
        return TopicUtils.getPathVariables(pattern, getTopic());
    }

    @Override
    @SuppressWarnings("all")
    public long hash(Object... objects) {
        if (payload instanceof Routable) {
            return ((Routable) payload).hash(objects);
        }
        return Routable.super.hash(objects);
    }

    @SuppressWarnings("all")
    public void copyRouteKeyToHeader() {
        if (payload instanceof Routable) {
            addHeader(Headers.routeKey.getKey(), ((Routable) payload).routeKey());
        }
    }

    @SuppressWarnings("all")
    @Override
    public Object routeKey() {
        if (payload instanceof Routable) {
           return ((Routable) payload).routeKey();
        }
        return headers.get(Headers.routeKey.getKey());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        SerializeUtils.writeObject(topic,out);
        SerializeUtils.writeObject(payload,out);
        SerializeUtils.writeKeyValue(headers,out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        topic = SerializeUtils.readObjectAs(in);
        topicText = null;
        payload = SerializeUtils.readObjectAs(in);
        SerializeUtils.readKeyValue(in,this::addHeader);
    }
}
