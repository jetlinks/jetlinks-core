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
 * 每次订阅从 Context 读取可选的 recorder/provider，显式 recorder 优先。
 * 没有观测绑定时只执行原业务；仅有原记录器时保留记录并跳过详细提取。
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
     * 从订阅 Context 获取可选的记录器，观察原操作生命周期；没有观测能力时仅执行业务。
     *
     * @param action    稳定且非空白的操作名
     * @param operation 原业务操作，仅在订阅时调用，不得返回 null
     * @param <T>       原业务输出类型
     * @return 保持原值、错误、空结果和取消语义的惰性 Mono
     * @since 1.3.3
     * @see RecorderProvider
     */
    public static <T> Mono<T> observeMono(String action, Supplier<? extends Mono<T>> operation) {
        Objects.requireNonNull(operation, "operation");
        return observeMono(ObservationSpec.<Void, T>of(action), null, ignored -> operation.get());
    }

    /**
     * 从订阅 Context 的 Recorder.class 读取原记录器，缺省为 noop；不查找 Monitor。
     * provider 可独立贡献附加接收端，两者均未启用时不运行提取或创建 action。
     *
     * @param spec      来源本地的不可变提取配置，不能为 null
     * @param input     实际业务输入，可以为 null，由业务操作和提取器解释
     * @param operation 原业务操作，仅在订阅时调用，不得返回 null
     * @param <I>       实际业务输入类型
     * @param <O>       原业务输出类型
     * @return 保持原值、错误、空结果和取消语义的惰性 Mono
     * @since 1.3.3
     * @see #observeMono(Recorder, ObservationSpec, Object, Function)
     */
    public static <I, O> Mono<O> observeMono(ObservationSpec<I, O> spec,
                                           I input,
                                           Function<? super I, ? extends Mono<O>> operation) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(operation, "operation");
        return Mono.deferContextual(context -> observe(recorder(context), spec, input, operation, context));
    }

    /**
     * 按订阅观察原操作生命周期，非 fatal 记录故障不改变业务信号。
     *
     * @param recorder  原记录器，优先于 Context 中的记录器；非空且工厂须返回未启动的新 action
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
     * @param recorder  原记录器，优先于 Context 中的记录器；非空且工厂须返回未启动的新 action
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
        return Mono.deferContextual(context -> observe(recorder, spec, input, operation, context));
    }

    private static <I, O> Mono<O> observe(Recorder recorder, ObservationSpec<I, O> spec,
                                         I input, Function<? super I, ? extends Mono<O>> operation,
                                         ContextView context) {
        Selection selection = select(spec.action(), context);
        if (recorder == Recorder.noop() && selection.value.additionalRecorder() == Recorder.noop()) {
            // 已处于 deferContextual 的订阅回调；空路径无需再包装 defer、组合器或提取状态。
            return operation.apply(input);
        }
        ActionRecorder<O> action = Recorder
            .combine(recorder, selection.value.additionalRecorder())
            .action(spec.action());
        if (action == ActionRecorder.<O>noop()) {
            return operation.apply(input);
        }
        action.tag(ObservationKeys.METADATA_STATUS, selection.failed
            ? ObservationKeys.FAILED
            : selection.value.captureMetadata() ? ObservationKeys.CAPTURED : ObservationKeys.NOT_REQUESTED);
        if (selection.value.policyVersion() != null) {
            action.tag(ObservationKeys.POLICY_VERSION, selection.value.policyVersion());
        }
        Mono<O> source = Mono.defer(() -> operation.apply(input));
        if (selection.value.captureMetadata() && (spec.inputWriter() != null || spec.outputWriter() != null)) {
            Capture capture = new Capture(action);
            capture.write(input, spec.inputWriter());
            if (spec.outputWriter() != null) {
                source = source.doOnNext(output -> capture.write(output, spec.outputWriter()));
            }
        }
        return source.transform(action);
    }

    private static Recorder recorder(ContextView context) {
        try {
            return context.getOrDefault(Recorder.class, Recorder.noop());
        } catch (Throwable error) {
            report("context recorder", error);
            return Recorder.noop();
        }
    }

    private static Selection select(String action, ContextView context) {
        try {
            RecorderProvider provider = context.getOrDefault(RecorderProvider.class, RecorderProvider.disabled());
            if (provider == RecorderProvider.disabled()) {
                return Selection.DISABLED;
            }
            RecorderSelection selection = Objects.requireNonNull(provider.select(action, context), "selection");
            return selection == RecorderSelection.disabled() ? Selection.DISABLED : new Selection(selection, false);
        } catch (Throwable error) {
            report("provider selection", error);
            return Selection.FAILED;
        }
    }

    private static void report(String phase, Throwable error) {
        Exceptions.throwIfFatal(error);
        LOGGER.warn("Monitor observation failed during {}", phase, error);
    }

    private record Selection(RecorderSelection value, boolean failed) {
        private static final Selection DISABLED = new Selection(RecorderSelection.disabled(), false);
        private static final Selection FAILED = new Selection(RecorderSelection.disabled(), true);
    }

    private static final class Capture {
        private final ActionRecorder<?> action;
        private final MetadataBuffer.Budget budget = new MetadataBuffer.Budget();
        private boolean failed;

        private Capture(ActionRecorder<?> action) {
            this.action = action;
        }

        private <T> void write(T value, BiConsumer<? super T, RecordMetadata> writer) {
            if (writer == null) {
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
