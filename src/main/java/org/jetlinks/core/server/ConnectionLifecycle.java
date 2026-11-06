package org.jetlinks.core.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Session-generation connection changes; a close is not necessarily a network failure. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionLifecycle {
    private long observedSince;
    private long opened;
    private long closed;
    private long lastChangeTime;
}
