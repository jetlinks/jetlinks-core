package org.jetlinks.core.monitor.observation;

import org.jetlinks.core.monitor.recorder.ActionRecorder;
import org.jetlinks.core.monitor.recorder.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 基于 Recorder 的无状态执行观测工具，不改变 Monitor 的能力接口。
 * 每次订阅从 Context 选择 provider；没有绑定时保留原记录器并跳过详细提取。
 * 不额外订阅、重试、创建 span 或管理后台队列，不提供业务提交或持久化保证。
 *
 * @since 1.3.3
 * @see RecorderProvider
 * @see ObservationSpec
 */
public final class Observations {

    private static final Logger LOGGER = LoggerFactory.getLogger(Observations.class);

    private Observations() {
    }

    /**
     * 按订阅观察原操作生命周期，非 fatal 记录故障不改变业务信号。
     *
     * @param recorder  原记录器，不能为 null；其工厂须返回未启动的新 action
     * @param action    稳定且非空白的操作名
     * @param operation 原业务操作，仅在订阅时调用，不得返回 null
     * @param <T>       原业务输出类型
     * @return 保持原值、错误、空结果和取消语义的惰性 Mono
     * @since 1.3.3
     */
    public static <T> Mono<T> observeMono(Recorder recorder, String action,
                                        Supplier<? extends Mono<T>> operation) {
        Objects.requireNonNull(operation, "operation");
        return observeMono(recorder, ObservationSpec.<Void, T>of(action), null, ignored -> operation.get());
    }

    /**
     * 一次包装原业务操作，详细提取仅在本次订阅的 provider 选择启用时执行。
     * 原或附加 action 独立创建和终结，元数据、provider 及记录回调的非 fatal 故障被隔离。
     *
     * @param recorder  原记录器，不能为 null；其工厂须返回未启动的新 action
     * @param spec      来源本地的不可变提取配置，不能为 null
     * @param input     实际业务输入，可以为 null，由业务操作和提取器解释
     * @param operation 原业务操作，仅在订阅时调用，不得返回 null
     * @param <I>       实际业务输入类型
     * @param <O>       原业务输出类型
     * @return 保持原值、错误、空结果和取消语义的惰性 Mono
     * @since 1.3.3
     */
    public static <I, O> Mono<O> observeMono(Recorder recorder,
                                           ObservationSpec<I, O> spec,
                                           I input,
                                           Function<? super I, ? extends Mono<O>> operation) {
        Objects.requireNonNull(recorder, "recorder");
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(operation, "operation");
        // 可重订阅 Publisher 只能复用配置，不能复用已终结的 action 或策略选择。
        return Mono.deferContextual(context -> {
            Selection selection = select(spec.action(), context);
            ActionRecorder<O> action = Recorder
                .combine(recorder, selection.value.additionalRecorder())
                .action(spec.action());
            if (action == ActionRecorder.<O>noop()) {
                return Mono.defer(() -> operation.apply(input));
            }
            action.tag(ObservationKeys.METADATA_STATUS, selection.failed
                ? ObservationKeys.FAILED
                : selection.value.captureMetadata() ? ObservationKeys.CAPTURED : ObservationKeys.NOT_REQUESTED);
            if (selection.value.policyVersion() != null) {
                action.tag(ObservationKeys.POLICY_VERSION, selection.value.policyVersion());
            }
            Capture capture = new Capture(action, selection.value.captureMetadata(), selection.failed);
            capture.write(input, spec.inputWriter());
            return Mono
                .defer(() -> operation.apply(input))
                .doOnNext(output -> capture.write(output, spec.outputWriter()))
                .transform(action);
        });
    }

    private static Selection select(String action, ContextView context) {
        try {
            RecorderProvider provider = context.getOrDefault(RecorderProvider.class, RecorderProvider.disabled());
            return new Selection(Objects.requireNonNull(provider.select(action, context), "selection"), false);
        } catch (Throwable error) {
            report("provider selection", error);
            return new Selection(RecorderSelection.disabled(), true);
        }
    }

    private static void report(String phase, Throwable error) {
        Exceptions.throwIfFatal(error);
        LOGGER.warn("Monitor observation failed during {}", phase, error);
    }

    private record Selection(RecorderSelection value, boolean failed) {
    }

    private static final class Capture {
        private final ActionRecorder<?> action;
        private final boolean enabled;
        private final MetadataBuffer.Budget budget = new MetadataBuffer.Budget();
        private boolean failed;

        private Capture(ActionRecorder<?> action, boolean enabled, boolean failed) {
            this.action = action;
            this.enabled = enabled;
            this.failed = failed;
        }

        private <T> void write(T value, BiConsumer<? super T, RecordMetadata> writer) {
            if (!enabled || writer == null) {
                return;
            }
            try {
                MetadataBuffer buffer = new MetadataBuffer();
                writer.accept(value, buffer);
                buffer.commit(action, budget);
            } catch (Throwable error) {
                report("metadata extraction", error);
                String status = error instanceof MetadataBuffer.LimitExceeded
                    ? ObservationKeys.TRUNCATED : ObservationKeys.FAILED;
                // 后续成功不能掩盖此前已知的缺口；普通失败比预算裁剪优先。
                if (!failed || ObservationKeys.FAILED.equals(status)) {
                    action.tag(ObservationKeys.METADATA_STATUS, status);
                }
                failed = true;
            }
        }
    }
}
