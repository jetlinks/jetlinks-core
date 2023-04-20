package org.jetlinks.core.trace;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.AllArgsConstructor;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.codec.Codecs;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.trace.data.SpanDataInfo;
import org.jetlinks.core.utils.StringBuilderUtils;
import reactor.core.publisher.Mono;

import java.util.Collection;

@AllArgsConstructor(staticName = "create")
public class EventBusSpanExporter implements SpanExporter {
    private final EventBus eventBus;

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {

        for (SpanData span : spans) {
            doPublish(span).subscribe();
        }
        return CompletableResultCode.ofSuccess();
    }

    //  /trace/{app}/{span}
    Mono<Void> doPublish(SpanData data) {
        String topic = StringBuilderUtils
                .buildString(data, (_data, builder) -> {
                    builder.append("/trace/")
                           .append(_data.getInstrumentationScopeInfo().getName());
                    if (!_data.getName().startsWith("/")) {
                        builder.append("/");
                    }
                    builder.append(_data.getName());
                });
        return eventBus
                .publish(topic, Mono.fromSupplier(() -> SpanDataInfo.of(data)))
                .then();
    }


    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
