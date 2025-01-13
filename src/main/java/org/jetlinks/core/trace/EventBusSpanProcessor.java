package org.jetlinks.core.trace;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import lombok.AllArgsConstructor;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.trace.data.SpanDataInfo;
import org.jetlinks.core.utils.TopicUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;

@AllArgsConstructor(staticName = "create")
public class EventBusSpanProcessor implements SpanProcessor {
    private final EventBus eventBus;

    private final Map<String, SharedPathString> prefixCache = new ConcurrentReferenceHashMap<>();

    @Override
    public void onStart(@Nonnull Context parentContext, @Nonnull ReadWriteSpan span) {
        // 推送start ?

    }

    @Override
    public boolean isStartRequired() {
        return false;
    }

    //  /trace/{appName}
    private SeparatedCharSequence prefix(ReadableSpan span) {
        return prefixCache
                .computeIfAbsent(
                        span.getInstrumentationScopeInfo().getName(),
                        (name) -> SharedPathString.of(new String[]{"", "trace", name})
                );
    }

    @Override
    public void onEnd(@Nonnull ReadableSpan span) {
        Context ctx = Context.current();

        CharSequence name = ctx.get(TraceHolder.SPAN_NAME);

        if (name == null) {
            name = SharedPathString.of(TopicUtils.split(span.getName(), true, false));
        }

        SeparatedCharSequence topic = this.prefix(span).append(name);

        eventBus
                .publish(topic, () -> SpanDataInfo.of(span.toSpanData()))
                .subscribe();
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }
}
