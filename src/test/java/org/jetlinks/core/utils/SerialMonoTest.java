package org.jetlinks.core.utils;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class SerialMonoTest {
    @Test
    public void monoPreservesContextDemandEmptyErrorAndSingleSubscription() {
        var serial = new SerialFlux<String>(1);
        var result = serial.join(Mono.deferContextual(ctx -> Mono.just(ctx.get("key"))));
        StepVerifier.create(result.contextWrite(Context.of("key", "value")), 0)
            .thenRequest(1).expectNext("value").verifyComplete();
        StepVerifier.create(result).expectError(IllegalStateException.class).verify();
        StepVerifier.create(serial.join(Mono.empty())).verifyComplete();
        StepVerifier.create(serial.join(Mono.error(new IllegalArgumentException("failed"))))
            .expectErrorMessage("failed").verify();
        assertEquals(0, serial.size());
    }

    @Test
    public void cancellationOverflowDisposalAndReentryKeepTheSameQueue() {
        var serial = new SerialFlux<Integer>(1);
        var source = Sinks.<Integer>one();
        var values = new ArrayList<Integer>();
        var cancelled = new AtomicInteger();
        var active = serial.join(source.asMono().doOnCancel(cancelled::incrementAndGet))
            .subscribe(value -> {
                values.add(value);
                serial.join(Mono.just(3)).subscribe(values::add);
            });
        var waiter = serial.join(Mono.just(2)).subscribe(values::add);
        StepVerifier.create(serial.join(Mono.just(9))).expectError(RejectedExecutionException.class).verify();
        waiter.dispose();
        assertTrue(values.isEmpty());
        source.tryEmitValue(1);
        assertEquals(List.of(1, 3), values);
        active.dispose();
        assertEquals(0, cancelled.get());
        assertEquals(0, serial.size());

        StepVerifier.create(serial.join(Mono.never().cast(Integer.class)))
            .then(serial::dispose).expectError(CancellationException.class).verify();
        StepVerifier.create(serial.join(Mono.just(4))).expectError(CancellationException.class).verify();
    }
}
