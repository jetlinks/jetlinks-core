package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import lombok.Setter;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultUpdateTingTagsMessage extends CommonThingMessage<DefaultUpdateTingTagsMessage> implements UpdateTingTagsMessage {

    @Setter
    private Map<String, Object> tags;

    public Map<String, Object> getTags() {
        return tags == null ? Collections.emptyMap() : tags;
    }

    public DefaultUpdateTingTagsMessage tag(String tag, Object value) {
        if (tags == null) {
            tags = new ConcurrentHashMap<>();
        }
        tags.put(tag, value);
        return this;
    }

    public DefaultUpdateTingTagsMessage tags(Map<String, Object> tags) {
        if (tags == null) {
            return this;
        }
        if (this.tags == null) {
            this.tags = new ConcurrentHashMap<>(tags);
            return this;
        }
        this.tags.putAll(tags);
        return this;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.UPDATE_TAG;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        this.tags = jsonObject.getJSONObject("tags");
    }
}
