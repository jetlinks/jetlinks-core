package org.jetlinks.core.monitor.observation;

import org.jetlinks.core.monitor.recorder.Recorder;

import java.util.Objects;

/**
 * 一次订阅的采集选择，不是业务执行记录或持久化回执。
 *
 * @param additionalRecorder 附加接收端，不能为 null；原记录器始终保留
 * @param captureMetadata    是否执行详细提取，与原记录器是否启用独立
 * @param policyVersion      本地有效策略版本，可空，仅供关联
 * @since 1.3.3
 */
public record RecorderSelection(Recorder additionalRecorder,
                                boolean captureMetadata,
                                String policyVersion) {

    private static final RecorderSelection DISABLED =
        new RecorderSelection(Recorder.noop(), false, null);

    public RecorderSelection {
        Objects.requireNonNull(additionalRecorder, "additionalRecorder");
    }

    public static RecorderSelection disabled() {
        return DISABLED;
    }

    /**
     * 使用 noop 附加接收端时，仅丰富原记录；调用方应确保至少有一个有效接收端。
     */
    public static RecorderSelection capture(Recorder additional, String policyVersion) {
        return new RecorderSelection(additional, true, policyVersion);
    }
}
