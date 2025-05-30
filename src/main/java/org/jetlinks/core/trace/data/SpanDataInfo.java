package org.jetlinks.core.trace.data;

import com.google.common.collect.Maps;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.jetlinks.core.utils.RecyclerUtils;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Getter
@Setter
public class SpanDataInfo implements Externalizable {
    private static final long serialVersionUID = -1;

    private String app;
    private String name;
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private long startWithNanos;
    private long endWithNanos;
    private Map<String, Object> attributes;
    private List<SpanEventDataInfo> events;

    private transient List<? extends SpanDataInfo> children;

    public static SpanDataInfo of(SpanData data) {
        return new SpanDataInfo().with(data);
    }

    public SpanDataInfo with(SpanData data) {
        this.app = data.getInstrumentationScopeInfo().getName();
        this.name = data.getName();
        this.traceId = data.getTraceId();
        this.spanId = data.getSpanId();
        this.parentSpanId = data.getParentSpanId();
        this.startWithNanos = data.getStartEpochNanos();
        this.endWithNanos = data.getEndEpochNanos();

        Attributes attr = data.getAttributes();
        if (!attr.isEmpty()) {
            attributes = Maps.newHashMapWithExpectedSize(attr.size());
            attr.forEach((k, v) -> {
                if (v instanceof Supplier) {
                    v = ((Supplier<?>) v).get();
                }
                attributes.put(k.getKey(), v);
            });
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
        out.writeByte(0x02);

        out.writeUTF(app);
        out.writeUTF(name);
        out.writeUTF(traceId);
        out.writeUTF(spanId);
        out.writeUTF(parentSpanId);
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
        int version = in.readUnsignedByte();

        this.app = RecyclerUtils.intern(in.readUTF());
        this.name = RecyclerUtils.intern(in.readUTF());
        this.traceId = in.readUTF();
        this.spanId = in.readUTF();
        this.parentSpanId = in.readUTF();
        this.startWithNanos = in.readLong();
        this.endWithNanos = in.readLong();
        this.attributes = SerializeUtils
            .readMap(in,
                     e -> RecyclerUtils.intern(String.valueOf(e)),
                     Function.identity(),
                     Maps::newHashMapWithExpectedSize);
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

    public StringBuilder toString(StringBuilder builder) {
        return toString(builder, "", true);
    }

    /**
     * 递归输出树状结构的链路追踪信息
     * @param builder 字符串构建器
     * @param prefix 当前层级的前缀字符串
     * @param isLast 是否为父节点的最后一个子节点
     * @return 字符串构建器
     */
    public StringBuilder toString(StringBuilder builder, String prefix, boolean isLast) {
        // 输出当前span的基本信息
        builder.append(prefix);
        if (!prefix.isEmpty()) {
            builder.append(isLast ? "└── " : "├── ");
        }
        builder.append("[").append(app != null ? app : "unknown").append("] ")
               .append(name != null ? name : "unknown");
        
        // 计算并输出耗时
        long durationMs = (endWithNanos - startWithNanos) / 1_000_000;
        builder.append(" (").append(durationMs).append("ms)").append("\n");
        
        // 计算子项的前缀
        String childPrefix = prefix + (isLast ? "    " : "│   ");
        
        // 输出attributes
        if (attributes != null && !attributes.isEmpty()) {
            builder.append(childPrefix).append("├── Attributes:\n");
            String attrPrefix = childPrefix + "│   ";
            int attrIndex = 0;
            int attrCount = attributes.size();
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                boolean isLastAttr = (attrIndex == attrCount - 1);
                builder.append(attrPrefix).append(isLastAttr ? "└── " : "├── ")
                       .append(entry.getKey()).append(": ");
                formatMultiLineValue(String.valueOf(entry.getValue()), 
                                   attrPrefix + (isLastAttr ? "    " : "│   "), 
                                   entry.getKey(), builder);
                builder.append("\n");
                attrIndex++;
            }
        }
        
        // 输出events
        if (events != null && !events.isEmpty()) {
            boolean hasAttributes = attributes != null && !attributes.isEmpty();
            boolean hasChildren = children != null && !children.isEmpty();
            String eventBranch = hasChildren ? "├── " : "└── ";
            
            builder.append(childPrefix).append(eventBranch).append("Events:\n");
            String eventPrefix = childPrefix + (hasChildren ? "│   " : "    ");
            
            for (int i = 0; i < events.size(); i++) {
                SpanEventDataInfo event = events.get(i);
                boolean isLastEvent = (i == events.size() - 1);
                builder.append(eventPrefix).append(isLastEvent ? "└── " : "├── ")
                       .append(event.getName());
                
                // 计算事件相对时间
                long relativeTimeMs = (event.getTimeNanos() - startWithNanos) / 1_000_000;
                builder.append(" (at ").append(relativeTimeMs).append("ms)");
                
                // 输出事件属性（如果有）
                if (event.getAttributes() != null && !event.getAttributes().isEmpty()) {
                    builder.append(" {");
                    boolean first = true;
                    for (Map.Entry<String, Object> attr : event.getAttributes().entrySet()) {
                        if (!first) builder.append(", ");
                        builder.append(attr.getKey()).append("=").append(attr.getValue());
                        first = false;
                    }
                    builder.append("}");
                }
                builder.append("\n");
            }
        }
        
        // 递归输出children
        if (children != null && !children.isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                SpanDataInfo child = children.get(i);
                boolean isLastChild = (i == children.size() - 1);
                child.toString(builder, childPrefix, isLastChild);
            }
        }
        
        return builder;
    }
    
    /**
     * 格式化多行文本，保持缩进对齐
     * @param value 原始文本值
     * @param prefix 缩进前缀
     * @param key 键
     * @param builder 字符串构建器
     */
    private void formatMultiLineValue(String value, String prefix, String key, StringBuilder builder) {
        if (value == null) {
            builder.append("null");
            return;
        }
        
        String[] lines = value.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                builder.append("\n").append(prefix);
                // 计算对齐位置：需要对齐到第一行值的位置
                // 第一行格式："├── multiline: 第一行" 
                // 第二行prefix格式："│   "，需要对齐到"第一行"的位置
                // 由于第二行prefix已经有"│   "（4个字符），只需要补充 key + ": " 的长度
                int alignLength = key.length() + 2; // key + ": "
                
                for (int j = 0; j < alignLength; j++) {
                    builder.append(" ");
                }
            }
            builder.append(lines[i]);
        }
    }
    
    /**
     * 标准toString方法，便于直接调用
     * @return 格式化的字符串
     */
    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
