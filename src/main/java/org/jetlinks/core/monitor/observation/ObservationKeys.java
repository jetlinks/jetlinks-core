package org.jetlinks.core.monitor.observation;

import org.jetlinks.core.Key;

/**
 * 框架采集状态，不能解释为业务成功、精确血缘或持久化确认。
 *
 * @since 1.3.3
 */
public final class ObservationKeys {

    /** 可选采集策略发布标识，可为版本号或摘要；不是传输协议格式版本。 */
    public static final Key<String> POLICY_VERSION = Key.stringKey("observation.policyVersion");
    public static final Key<String> METADATA_STATUS = Key.stringKey("observation.metadataStatus");
    public static final Key<String> EXECUTION_ID = Key.stringKey("observation.executionId");
    public static final Key<String> PARENT_EXECUTION_ID = Key.stringKey("observation.parentExecutionId");
    public static final Key<String> SOURCE_TYPE = Key.stringKey("observation.sourceType");
    public static final Key<String> SOURCE_ID = Key.stringKey("observation.sourceId");

    public static final String NOT_REQUESTED = "notRequested";
    public static final String CAPTURED = "captured";
    public static final String FAILED = "failed";
    public static final String TRUNCATED = "truncated";

    private ObservationKeys() {
    }
}
