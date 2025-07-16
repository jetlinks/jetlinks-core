package org.jetlinks.core.trace.data;

import com.google.common.collect.Maps;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(title = "应用标识")
    private String app;

    @Schema(title = "名称")
    private String name;

    @Schema(title = "链路ID")
    private String traceId;

    @Schema(title = "SpanId")
    private String spanId;

    @Schema(title = "上级SpanID")
    private String parentSpanId;

    @Schema(title = "开始时间(纳秒)")
    private long startWithNanos;

    @Schema(title = "结束时间(纳秒)")
    private long endWithNanos;

    @Schema(title = "链路属性")
    private Map<String, Object> attributes;

    @Schema(title = "事件")
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
        builder.append("🔍 [").append(app != null ? app : "unknown").append("] ")
               .append(name != null ? name : "unknown");
        
        // 计算并输出耗时
        long durationMs = (endWithNanos - startWithNanos) / 1_000_000;
        builder.append(" (").append(durationMs).append("ms)").append("\n");
        
        // 计算子项的前缀
        String childPrefix = prefix + (isLast ? "    " : "│   ");
        
        // 输出attributes
        if (attributes != null && !attributes.isEmpty()) {
            builder.append(childPrefix).append("├── 🏷️ Attributes:\n");
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
            
            builder.append(childPrefix).append(eventBranch).append("⚠️ Events:\n");
            String eventPrefix = childPrefix + (hasChildren ? "│   " : "    ");
            
            for (int i = 0; i < events.size(); i++) {
                SpanEventDataInfo event = events.get(i);
                boolean isLastEvent = (i == events.size() - 1);
                
                // 根据事件名称选择合适的emoji
                String eventEmoji = getEventEmoji(event.getName());
                
                builder.append(eventPrefix).append(isLastEvent ? "└── " : "├── ")
                       .append(eventEmoji).append(" ").append(event.getName());
                
                // 计算事件相对时间
                long relativeTimeMs = (event.getTimeNanos() - startWithNanos) / 1_000_000;
                builder.append(" (at ").append(relativeTimeMs).append("ms)").append("\n");
                
                // 输出事件属性（采用与span attributes相同的格式）
                if (event.getAttributes() != null && !event.getAttributes().isEmpty()) {
                    String eventAttrPrefix = eventPrefix + (isLastEvent ? "    " : "│   ");
                    int attrIndex = 0;
                    int attrCount = event.getAttributes().size();
                    for (Map.Entry<String, Object> attr : event.getAttributes().entrySet()) {
                        boolean isLastEventAttr = (attrIndex == attrCount - 1);
                        builder.append(eventAttrPrefix).append(isLastEventAttr ? "└── " : "├── ")
                               .append(attr.getKey()).append(": ");
                        formatMultiLineValue(String.valueOf(attr.getValue()), 
                                           eventAttrPrefix + (isLastEventAttr ? "    " : "│   "), 
                                           attr.getKey(), builder);
                        builder.append("\n");
                        attrIndex++;
                    }
                }
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

    /**
     * 根据事件名称选择合适的emoji图标
     * @param eventName 事件名称
     * @return emoji图标
     */
    private String getEventEmoji(String eventName) {
        if (eventName == null) {
            return "📝";
        }
        
        String lowerName = eventName.toLowerCase();
        
        // 错误相关事件
        if (lowerName.contains("error") || lowerName.contains("fail") || lowerName.contains("exception")) {
            return "❌";
        }
        
        // 警告相关事件
        if (lowerName.contains("warn") || lowerName.contains("timeout") || lowerName.contains("retry")) {
            return "⚠️";
        }
        
        // 开始相关事件
        if (lowerName.contains("start") || lowerName.contains("begin") || lowerName.contains("init")) {
            return "🚀";
        }
        
        // 完成相关事件
        if (lowerName.contains("finish") || lowerName.contains("complete") || lowerName.contains("end") || lowerName.contains("success")) {
            return "✅";
        }
        
        // 数据库相关事件
        if (lowerName.contains("sql") || lowerName.contains("query") || lowerName.contains("database") || lowerName.contains("db")) {
            return "🗄️";
        }
        
        // 网络相关事件
        if (lowerName.contains("request") || lowerName.contains("response") || lowerName.contains("http") || lowerName.contains("api")) {
            return "🌐";
        }
        
        // 认证相关事件
        if (lowerName.contains("auth") || lowerName.contains("login") || lowerName.contains("logout") || lowerName.contains("permission")) {
            return "🔐";
        }
        
        // 缓存相关事件
        if (lowerName.contains("cache") || lowerName.contains("redis") || lowerName.contains("memory")) {
            return "💾";
        }
        
        // 日志相关事件
        if (lowerName.contains("log") || lowerName.contains("audit") || lowerName.contains("record")) {
            return "📋";
        }
        
        // 业务逻辑相关事件
        if (lowerName.contains("business") || lowerName.contains("logic") || lowerName.contains("process") || lowerName.contains("execute")) {
            return "⚙️";
        }
        
        // 默认图标
        return "📝";
    }
}
