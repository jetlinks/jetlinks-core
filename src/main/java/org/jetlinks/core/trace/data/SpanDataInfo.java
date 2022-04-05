package org.jetlinks.core.trace.data;

import com.google.common.collect.Maps;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;

@Getter
@Setter
public class SpanDataInfo implements Externalizable {
    private static final long serialVersionUID = -1;

    private String app;
    private String name;
    private String traceId;
    private long startWithNanos;
    private long endWithNanos;
    private Map<String, Object> attributes;
    private List<SpanEventDataInfo> events;

    public static SpanDataInfo of(SpanData data) {
        return new SpanDataInfo().with(data);
    }

    public SpanDataInfo with(SpanData data) {
        this.app = data.getInstrumentationLibraryInfo().getName();
        this.name = data.getName();
        this.traceId = data.getTraceId();
        this.startWithNanos = data.getStartEpochNanos();
        this.endWithNanos = data.getEndEpochNanos();

        Attributes attr = data.getAttributes();
        if (!attr.isEmpty()) {
            attributes = Maps.newHashMapWithExpectedSize(attr.size());
            attr.forEach((k, v) -> attributes.put(k.getKey(), v));
        }
        List<EventData> eventData = data.getEvents();
        if (CollectionUtils.isNotEmpty(eventData)) {
            this.events = new ArrayList<>();
            for (EventData eventDatum : eventData) {
                this.events.add(SpanEventDataInfo.of(eventDatum));
            }
        }
        return this;
    }

    public Optional<SpanEventDataInfo> getEvent(String name) {
        if (events == null) {
            return Optional.empty();
        }
        for (SpanEventDataInfo eventInfo : events) {
            if (Objects.equals(name, eventInfo.getName())) {
                return Optional.of(eventInfo);
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("all")
    public <T> Optional<T> getAttribute(String key) {
        if (attributes == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) attributes.get(key));
    }

    public <T> Optional<T> getAttribute(AttributeKey<T> key) {
        return getAttribute(key.getKey());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(app);
        out.writeUTF(name);
        out.writeUTF(traceId);
        out.writeLong(startWithNanos);
        out.writeLong(endWithNanos);
        SerializeUtils.writeKeyValue(attributes, out);
        if (events == null) {
            out.writeInt(0);
        } else {
            out.writeInt(events.size());
            for (SpanEventDataInfo event : events) {
                event.writeExternal(out);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.app = in.readUTF();
        this.name = in.readUTF();
        this.traceId = in.readUTF();
        this.startWithNanos = in.readLong();
        this.endWithNanos = in.readLong();
        this.attributes = SerializeUtils.<Object>readMap(in, Maps::newHashMapWithExpectedSize);
        int eventSize = in.readInt();
        if (eventSize > 0) {
            events = new ArrayList<>(eventSize);
            for (int i = 0; i < eventSize; i++) {
                SpanEventDataInfo dataInfo = new SpanEventDataInfo();
                dataInfo.readExternal(in);
                events.add(dataInfo);
            }
        }
    }
}
