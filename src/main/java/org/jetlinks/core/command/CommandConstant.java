package org.jetlinks.core.command;

import org.jetlinks.core.config.ConfigKey;

public interface CommandConstant {

    ConfigKey<Boolean> UNBOUNDED = ConfigKey.of("unbounded", "无界流", Boolean.class);

    ConfigKey<Boolean> responseFlux = ConfigKey.of("responseFlux", "Flux响应", Boolean.class);
}
