package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import lombok.Setter;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UpdateTagMessage extends CommonDeviceMessage<UpdateTagMessage> implements UpdateTingTagsMessage {

    @Setter
    private Map<String, Object> tags;

    public Map<String, Object> getTags() {
        return tags == null ? Collections.emptyMap() : tags;
    }

    public synchronized UpdateTagMessage tag(String tag, Object value) {
        if (tags == null) {
            tags = new ConcurrentHashMap<>();
        }
        tags.put(tag, value);
        return this;
    }

    public synchronized UpdateTagMessage tags(Map<String, Object> tags) {
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

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        UpdateTingTagsMessage.super.writeExternal(out);
        SerializeUtils.writeKeyValue(tags,out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        UpdateTingTagsMessage.super.readExternal(in);
        SerializeUtils.readKeyValue(in,this::tag);
    }
}
