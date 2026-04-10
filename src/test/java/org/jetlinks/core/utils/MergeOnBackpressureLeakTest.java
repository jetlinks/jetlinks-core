package org.jetlinks.core.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class MergeOnBackpressureLeakTest {

    @Test
    public void testLeakOnCancel() {
        AtomicInteger releasedCount = new AtomicInteger(0);
        TestPublisher<ByteBuf> source = TestPublisher.createNoncompliant(TestPublisher.Violation.REQUEST_OVERFLOW);
        List<ByteBuf> bufs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ByteBuf buf = Unpooled.buffer(10).writeInt(i + 1);
            bufs.add(buf);
        }

        Supplier<List<ByteBuf>> containerSupplier = ArrayList::new;
        BiFunction<List<ByteBuf>, ByteBuf, List<ByteBuf>> merger = (list, item) -> {
            list.add(item);
            return list;
        };
        Predicate<List<ByteBuf>> bufferPredicate = list -> list.size() < 5;
        Function<List<ByteBuf>, List<ByteBuf>> mapper = ArrayList::new;

        Flux<List<ByteBuf>> flux = FluxUtils.mergeOnBackpressure(
            source.flux(),
            containerSupplier,
            merger,
            bufferPredicate,
            mapper,
            buf -> {
                releasedCount.incrementAndGet();
                ReferenceCountUtil.safeRelease(buf);
            }
        );

        StepVerifier.create(flux, 0)
            .then(() -> bufs.forEach(source::next))
            .thenRequest(1)
            .assertNext(list -> {
                assertEquals(5, list.size());
                list.forEach(ReferenceCountUtil::safeRelease);
            })
            .thenCancel()
            .verify();

        assertEquals(5, releasedCount.get());

        for (int i = 0; i < 5; i++) {
            assertEquals(0, bufs.get(i).refCnt());
        }
        for (int i = 5; i < 10; i++) {
            assertEquals(0, bufs.get(i).refCnt());
        }
    }

    @Test
    public void testLeakOnMapperError() {
        AtomicInteger releasedCount = new AtomicInteger(0);
        ByteBuf buf1 = Unpooled.buffer(10).writeInt(1);
        ByteBuf buf2 = Unpooled.buffer(10).writeInt(2);

        Supplier<List<ByteBuf>> containerSupplier = ArrayList::new;
        BiFunction<List<ByteBuf>, ByteBuf, List<ByteBuf>> merger = (list, item) -> {
            list.add(item);
            return list;
        };
        Predicate<List<ByteBuf>> bufferPredicate = list -> list.size() < 2;

        Flux<List<ByteBuf>> flux = FluxUtils.mergeOnBackpressure(
            Flux.just(buf1, buf2),
            containerSupplier,
            merger,
            bufferPredicate,
            list -> {
                throw new IllegalStateException("mapper error");
            },
            buf -> {
                releasedCount.incrementAndGet();
                ReferenceCountUtil.safeRelease(buf);
            }
        );

        StepVerifier.create(flux)
            .expectErrorMessage("mapper error")
            .verify();

        assertEquals(0, buf1.refCnt());
        assertEquals(0, buf2.refCnt());
        assertEquals(2, releasedCount.get());
    }

    @Test
    public void testLeakOnBufferPredicateError() {
        AtomicInteger releasedCount = new AtomicInteger(0);
        ByteBuf buf1 = Unpooled.buffer(10).writeInt(1);

        Supplier<List<ByteBuf>> containerSupplier = ArrayList::new;
        BiFunction<List<ByteBuf>, ByteBuf, List<ByteBuf>> merger = (list, item) -> {
            list.add(item);
            return list;
        };
        Function<List<ByteBuf>, List<ByteBuf>> mapper = ArrayList::new;

        Flux<List<ByteBuf>> flux = FluxUtils.mergeOnBackpressure(
            Flux.just(buf1),
            containerSupplier,
            merger,
            list -> {
                throw new IllegalStateException("predicate error");
            },
            mapper,
            buf -> {
                releasedCount.incrementAndGet();
                ReferenceCountUtil.safeRelease(buf);
            }
        );

        StepVerifier.create(flux)
            .expectErrorMessage("predicate error")
            .verify();

        assertEquals(0, buf1.refCnt());
        assertEquals(1, releasedCount.get());
    }

    @Test
    public void testDoOnDiscardReceivesSkipAndCleanupDiscard() {
        AtomicInteger releasedCount = new AtomicInteger(0);
        TestPublisher<ByteBuf> source = TestPublisher.createNoncompliant(TestPublisher.Violation.REQUEST_OVERFLOW);
        List<ByteBuf> bufs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            bufs.add(Unpooled.buffer(10).writeInt(i + 1));
        }
        Supplier<List<ByteBuf>> containerSupplier = ArrayList::new;
        Function<List<ByteBuf>, List<ByteBuf>> mapper = ArrayList::new;

        Flux<List<ByteBuf>> flux = FluxUtils
            .mergeOnBackpressure(
                source.flux().skip(1),
                containerSupplier,
                (list, item) -> {
                    list.add(item);
                    return list;
                },
                list -> list.size() < 5,
                mapper,
                null
            )
            .doOnDiscard(ByteBuf.class, buf -> {
                releasedCount.incrementAndGet();
                ReferenceCountUtil.safeRelease(buf);
            });

        StepVerifier.create(flux, 0)
            .then(() -> bufs.forEach(source::next))
            .thenRequest(1)
            .assertNext(list -> {
                assertEquals(5, list.size());
                list.forEach(ReferenceCountUtil::safeRelease);
            })
            .thenCancel()
            .verify();

        assertEquals(5, releasedCount.get());
        bufs.forEach(buf -> assertEquals(0, buf.refCnt()));
    }
}
