package org.jetlinks.core.monitor.observation;

import org.jetlinks.core.Key;
import org.jetlinks.core.things.ThingId;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 执行所涉及的主体、实际数据及定义依据的统一声明。
 *
 * 由来源的 {@link ObservationSpec} 提取器调用，只复制已有引用，不查数据或保存业务对象。
 * 引用的类型负责提供原记录/不可变快照的读取能力；生成结果不代表事务提交。
 * 框架入口可先绑定主体供采集选择，主体不参与授权，也不进入通用传输头。
 *
 * @since 1.3.3
 * @see RecordMetadata
 */
public final class ObservationData {

    public static final Key<ThingId> SUBJECT = Key.create("data.subject");
    public static final Key<List<ThingId>> INPUTS = Key.listKey("data.inputs");
    public static final Key<List<ThingId>> OUTPUTS = Key.listKey("data.outputs");
    public static final Key<List<ThingId>> DEFINITIONS = Key.listKey("data.definitions");
    public static final Key<ThingId> RECORD = Key.create("data.record");
    public static final Key<Boolean> INPUTS_COMPLETE = Key.booleanKey("data.inputsComplete");

    private ObservationData() {
    }

    /** 入口在策略选择前绑定已认证主体；不从业务 payload 推断。 */
    public static Context withSubject(Context context, ThingId subject) {
        return context.put(SUBJECT, snapshot(subject));
    }

    /** 返回独立引用，不向调用方暴露 Context 中的可变 ThingId。 */
    public static Optional<ThingId> currentSubject(ContextView context) {
        return Optional.ofNullable(context.<ThingId>getOrDefault(SUBJECT, null))
                       .map(ObservationData::snapshot);
    }

    public static void subject(RecordMetadata metadata, ThingId subject) {
        metadata.attribute(SUBJECT, snapshot(subject));
    }

    /**
     * 只声明真正参与本阶段处理的输入。大量样本使用来源清单引用。
     * 逐项写入复用 RecordMetadata 的累计预算，超限立即停止，不先复制无界集合。
     */
    public static void used(RecordMetadata metadata, Collection<ThingId> inputs) {
        for (ThingId input : inputs) {
            metadata.attribute(INPUTS, List.of(snapshot(input)));
        }
    }

    public static void generated(RecordMetadata metadata, ThingId output) {
        metadata.attribute(OUTPUTS, List.of(snapshot(output)));
    }

    /** 定义引用必须能读取实际使用的旧内容，不能只有当前配置 ID。 */
    public static void definition(RecordMetadata metadata, ThingId definition) {
        metadata.attribute(DEFINITIONS, List.of(snapshot(definition)));
    }

    public static void record(RecordMetadata metadata, ThingId record) {
        metadata.attribute(RECORD, snapshot(record));
    }

    /** 来源声明本阶段输入已列全；缺省为未知，不证明上游覆盖或持久化。 */
    public static void inputsComplete(RecordMetadata metadata) {
        metadata.attribute(INPUTS_COMPLETE, true);
    }

    private static ThingId snapshot(ThingId reference) {
        return ThingId.of(reference.getType(), reference.getId());
    }
}
