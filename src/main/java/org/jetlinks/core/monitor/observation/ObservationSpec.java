package org.jetlinks.core.monitor.observation;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * 来源本地的不可变提取配置，可在不同订阅之间安全复用。
 *
 * 回调仅在详细采集启用时执行；输入须为实际选择的数据，输出出现不表示已持久化。
 * 不要求业务 DTO 实现接口，也不扫描或序列化业务对象。
 *
 * @param <I> 实际业务输入
 * @param <O> 原业务输出
 * @since 1.3.3
 */
public final class ObservationSpec<I, O> {

    private final String action;
    private final BiConsumer<? super I, RecordMetadata> inputWriter;
    private final BiConsumer<? super O, RecordMetadata> outputWriter;

    private ObservationSpec(String action,
                            BiConsumer<? super I, RecordMetadata> inputWriter,
                            BiConsumer<? super O, RecordMetadata> outputWriter) {
        this.action = Objects.requireNonNull(action, "action");
        if (action.isBlank()) {
            throw new IllegalArgumentException("action must not be blank");
        }
        this.inputWriter = inputWriter;
        this.outputWriter = outputWriter;
    }

    public static <I, O> ObservationSpec<I, O> of(String action) {
        return new ObservationSpec<>(action, null, null);
    }

    public ObservationSpec<I, O> onInput(BiConsumer<? super I, RecordMetadata> writer) {
        return new ObservationSpec<>(action, Objects.requireNonNull(writer, "writer"), outputWriter);
    }

    public ObservationSpec<I, O> onOutput(BiConsumer<? super O, RecordMetadata> writer) {
        return new ObservationSpec<>(action, inputWriter, Objects.requireNonNull(writer, "writer"));
    }

    public String action() {
        return action;
    }

    BiConsumer<? super I, RecordMetadata> inputWriter() {
        return inputWriter;
    }

    BiConsumer<? super O, RecordMetadata> outputWriter() {
        return outputWriter;
    }
}
