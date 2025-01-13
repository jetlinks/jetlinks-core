package org.jetlinks.core.trace;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.AllArgsConstructor;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.trace.data.SpanDataInfo;
import org.jetlinks.core.utils.TopicUtils;
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
    Mono<Long> doPublish(SpanData data) {
//        String topic = StringBuilderUtils
//            .buildString(data, (_data, builder) -> {
//                builder.append("/trace/")
//                       .append(_data.getInstrumentationScopeInfo().getName());
//                if (!_data.getName().startsWith("/")) {
//                    builder.append("/");
//                }
//                builder.append(_data.getName());
//            });

        SeparatedCharSequence topic = SharedPathString
            .of(new String[]{"", "trace", data.getInstrumentationScopeInfo().getName()})
            .append(SharedPathString.of(TopicUtils.split(data.getName(), true, false)));

        return eventBus
            .publish(topic, () -> SpanDataInfo.of(data));
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
