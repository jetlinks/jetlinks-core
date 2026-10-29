package org.jetlinks.core.monitor.observation;

import org.jetlinks.core.Key;

/**
 * 框架采集状态，不能解释为业务成功、精确血缘或持久化确认。
 *
 * @since 1.3.3
 */
public final class ObservationKeys {

    public static final Key<String> POLICY_VERSION = Key.stringKey("observation.policyVersion");
    public static final Key<String> METADATA_STATUS = Key.stringKey("observation.metadataStatus");

    public static final String NOT_REQUESTED = "notRequested";
    public static final String CAPTURED = "captured";
    public static final String FAILED = "failed";
    public static final String TRUNCATED = "truncated";

    private ObservationKeys() {
    }
}
