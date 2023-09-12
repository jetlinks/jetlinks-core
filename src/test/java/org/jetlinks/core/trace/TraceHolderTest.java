package org.jetlinks.core.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import lombok.SneakyThrows;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class TraceHolderTest {


    static {
        createTelemetry();
    }

    @Test
    @SneakyThrows
    public void testMono() {


        TraceMono.trace(Mono.just(1).name("test123"))
                 .onNext((span, integer) -> span.setAttributeLazy(AttributeKey.stringKey("test"), () -> "123"))
                 .subscribe(System.out::println);


        Mono.just(1)
            .as(MonoTracer
                        .create("iot-service",
                                "sendMessage",
                                (span, val) -> span.setAttribute("value", val))
            )
            .delayElement(Duration.ofSeconds(3))
            .map(i -> i + 10)
            .as(MonoTracer
                        .create("iot-service",
                                "sendMessage2",
                                (span, val) -> span.setAttribute("value", val))
            )
            .subscribe(System.out::println);

        Thread.sleep(10000);
    }

    @Test
    @SneakyThrows
    public void testFlux() {

        Map<String, String> map = new HashMap<>();
        map.put("traceparent", "00-1c7639bf41f3291edccd49ae7a1235c6-498329cea58d365a-01");

        TraceHolder
                .writeContextTo(map, Map::put)
                .thenMany(Flux.range(1, 10))
                //.doOnNext(System.out::println)
                .delayElements(Duration.ofMillis(100))
                .map(i -> i + 10)
                .flatMap(i -> Mono
                        .just(i)
                        .as(MonoTracer
                                    .create("iot-service",
                                            "gateway.device." + i,
                                            (span, val) -> span.setAttribute("value", val))
                        ))
                .as(FluxTracer
                            .create("iot-service",
                                    "sendMessage",
                                    (span, val) -> span.setAttribute("value", val))
                )
                .doOnNext(System.out::println)
                .then()
                .as(MonoTracer.createWith(map))
                .block();
        System.out.println(map);
        Thread.sleep(2000);

    }

    @Test
    public void testCreate() {
        Map<String, String> map = new HashMap<>();
        map.put("traceparent", "00-1c7639bf41f3291edccd49ae7a1235c6-498329cea58d365a-01");

        Mono.just(1)
            .as(MonoTracer.create("test", (
                    (span, data) -> {
                        System.out.println(span.getSpanContext().getTraceId());
                    }
            )))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }


    public static OpenTelemetry createTelemetry() {
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider
                .builder()
                .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                .build();

        return OpenTelemetrySdk
                .builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
    }
}