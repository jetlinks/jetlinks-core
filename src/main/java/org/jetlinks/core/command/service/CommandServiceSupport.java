package org.jetlinks.core.command.service;

import reactor.core.publisher.Mono;

public interface CommandServiceSupport {

    /**
     * 获取服务描述
     *
     * @return 服务描述
     */
    Mono<ServiceDescription> getDescription();

}
