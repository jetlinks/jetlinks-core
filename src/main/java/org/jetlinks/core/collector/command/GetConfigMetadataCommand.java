package org.jetlinks.core.collector.command;

import org.jetlinks.core.command.AbstractCommand;
import org.jetlinks.core.metadata.PropertyMetadata;
import reactor.core.publisher.Mono;

import java.util.List;

public abstract class GetConfigMetadataCommand<Self extends GetConfigMetadataCommand<Self>>
    extends AbstractCommand<Mono<List<PropertyMetadata>>, Self> {
}
