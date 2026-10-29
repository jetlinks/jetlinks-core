package org.jetlinks.core.monitor.observation;

import org.jetlinks.core.monitor.Monitor;
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

/**
 * Monitor 默认执行入口的无状态实现。没有全局采集状态、队列或业务上下文缓存。
 *
 * @since 1.3.3
 */
public final class ObservationSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservationSupport.class);

    private ObservationSupport() {
    }

    public static <I, O> Mono<O> observe(Monitor monitor,
                                        ObservationSpec<I, O> spec,
                                        I input,
                                        Function<? super I, ? extends Mono<O>> operation) {
        Objects.requireNonNull(monitor, "monitor");
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(operation, "operation");
        // 可重订阅 Publisher 只能复用配置，不能复用已终结的 action 或策略选择。
        return Mono.deferContextual(context -> {
            Selection selection = select(monitor, spec.action(), context);
            Recorder original = originalRecorder(monitor);
            ActionRecorder<O> action = Recorder
                .combine(original, selection.value.additionalRecorder())
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

    private static Selection select(Monitor monitor, String action, ContextView context) {
        try {
            RecorderProvider provider = context.hasKey(RecorderProvider.class)
                ? context.get(RecorderProvider.class)
                : monitor.recorderProvider();
            return new Selection(Objects.requireNonNull(provider.select(action, context), "selection"), false);
        } catch (Throwable error) {
            report("provider selection", error);
            return new Selection(RecorderSelection.disabled(), true);
        }
    }

    private static Recorder originalRecorder(Monitor monitor) {
        try {
            return Objects.requireNonNull(monitor.recorder(), "recorder");
        } catch (Throwable error) {
            report("original recorder", error);
            return Recorder.noop();
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
