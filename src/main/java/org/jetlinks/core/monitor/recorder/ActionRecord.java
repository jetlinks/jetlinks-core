package org.jetlinks.core.monitor.recorder;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.utils.ExceptionUtils;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class ActionRecord implements Externalizable {

    private String id;
    private String parentId;

    private CharSequence action;

    private Map<String, Object> tags;
    private Map<String, Object> attributes;

    private boolean hasError;

    private long valueCount;

    private boolean cancel;

    private String traceId;

    private String spanId;

    private long timestamp;
    private long useNanos;

    private String errorType;
    private String errorDetail;

    public Map<String, Object> writableTags() {
        return tags == null ? tags = new ConcurrentHashMap<>() : tags;
    }

    public Map<String, Object> writableAttributes() {
        return attributes == null ? attributes = new ConcurrentHashMap<>() : attributes;
    }

    public void withAttributes(String key, Object value) {
        if (key == null || value == null) {
            return;
        }
        writableAttributes().put(key, value);
    }

    public void withAttributes(Map<String, Object> data) {
        data.forEach(this::withAttributes);
    }

    public void withTag(String tag, Object value) {
        if (tag == null || value == null) {
            return;
        }
        writableTags().put(tag, value);
    }

    public void withTags(Map<String, Object> tags) {
        tags.forEach(this::withTag);
    }

    public void withValue(Object value) {
        valueCount++;
    }

    public void withError(Throwable error) {
        hasError = true;
        this.errorType = error.getClass().getTypeName();
        this.errorDetail = ExceptionUtils.getStackTrace(error);
    }

    public void end(long useNanos) {
        this.useNanos = useNanos;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        SerializeUtils.writeObject(action, out);
        SerializeUtils.writeObject(tags, out);
        SerializeUtils.writeObject(attributes, out);
        out.writeLong(valueCount);
        out.writeBoolean(hasError);
        out.writeBoolean(cancel);
        out.writeLong(timestamp);
        out.writeLong(useNanos);

        SerializeUtils.writeObject(traceId, out);
        SerializeUtils.writeObject(spanId, out);
        SerializeUtils.writeObject(errorType, out);
        SerializeUtils.writeObject(errorDetail, out);

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        action = SerializeUtils.readObjectAs(in);
        tags = SerializeUtils.readObjectAs(in);
        attributes = SerializeUtils.readObjectAs(in);
        valueCount = in.readLong();
        hasError = in.readBoolean();
        cancel = in.readBoolean();
        timestamp = in.readLong();
        useNanos = in.readLong();

        traceId = SerializeUtils.readObjectAs(in);
        spanId = SerializeUtils.readObjectAs(in);
        errorType = SerializeUtils.readObjectAs(in);
        errorDetail = SerializeUtils.readObjectAs(in);
    }
}
