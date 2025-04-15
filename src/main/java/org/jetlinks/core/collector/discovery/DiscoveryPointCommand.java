package org.jetlinks.core.collector.discovery;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetlinks.core.command.AbstractCommand;
import reactor.core.publisher.Flux;

@Schema(title = "发现点位信息")
public class DiscoveryPointCommand extends AbstractCommand<Flux<PointNode>,DiscoveryPointCommand> {


}
