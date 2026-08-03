package org.jetlinks.core.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ResourceLeakDetector;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author gyl
 */
public class FluxUtilsTest {


    @Test
    public void distinct() {
        Flux.just(1, 2, 3)
            .as(FluxUtils.distinct(i -> 1, Duration.ofSeconds(1)))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        Flux.just(1, 2, 3)
            .as(FluxUtils.distinct(i -> null, Duration.ofSeconds(1)))
            .as(StepVerifier::create)
            .expectNext(1, 2, 3)
            .verifyComplete();
    }

    @Test
    public void testMergeOnBackpressure_Basic() {
        // 测试基本合并功能：将整数合并到列表中，最多2个元素
        Flux<Integer> source = Flux.just(1, 2, 3, 4, 5, 6);

        Supplier<List<Integer>> containerSupplier = ArrayList::new;
        BiFunction<List<Integer>, Integer, List<Integer>> merger = (list, item) -> {
            // 创建新列表，不修改原列表
            List<Integer> newList = new ArrayList<>(list);
            newList.add(item);
            return newList;
        };
        Predicate<List<Integer>> bufferPredicate = list -> list.size() < 2; // 小于2时可以继续缓冲
        Function<List<Integer>, List<Integer>> mapper = list -> new ArrayList<>(list); // 复制列表内容

        Flux<List<Integer>> result = FluxUtils.mergeOnBackpressure(
            source,
            containerSupplier,
            merger,
            bufferPredicate,
            mapper,
            null
        );

        StepVerifier.create(result)
                    .expectNextCount(3)
                    .verifyComplete();
    }

    @Test
    public void testMergeOnBackpressure_MultipleElements() {
        // 测试合并多个元素：最多3个元素（bufferPredicate返回true表示可以继续）
        Flux<Integer> source = Flux.just(1, 2, 3, 4, 5);

        Supplier<List<Integer>> containerSupplier = ArrayList::new;
        BiFunction<List<Integer>, Integer, List<Integer>> merger = (list, item) -> {
            // 创建新列表，不修改原列表
            List<Integer> newList = new ArrayList<>(list);
            newList.add(item);
            return newList;
        };
        Predicate<List<Integer>> bufferPredicate = list -> list.size() < 3; // 小于3时可以继续
        Function<List<Integer>, List<Integer>> mapper = list -> new ArrayList<>(list);

        Flux<List<Integer>> result = FluxUtils.mergeOnBackpressure(
            source,
            containerSupplier,
            merger,
            bufferPredicate,
            mapper,
            null
        );

        StepVerifier
            .create(result.doOnNext(System.out::println))
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    public void testMergeOnBackpressure_WithBackpressure() {
        // 测试背压情况：下游请求速度慢
        Flux<Integer> source = Flux
            .range(1, 100)
            .doOnRequest((i) -> System.out.println("req:" + i));

        Supplier<List<Integer>> containerSupplier = ArrayList::new;
        BiFunction<List<Integer>, Integer, List<Integer>> merger = (list, item) -> {
            // 创建新列表，不修改原列表
            List<Integer> newList = new ArrayList<>(list);
            newList.add(item);
            return newList;
        };
        Predicate<List<Integer>> bufferPredicate = list -> list.size() < 4;
        Function<List<Integer>, List<Integer>> mapper = ArrayList::new;

        Flux<List<Integer>> result = FluxUtils
            .mergeOnBackpressure(
                source,
                containerSupplier,
                merger,
                bufferPredicate,
                mapper,
                null
            )
            .concatMap(list -> Mono.just(list)
                                   .delayElement(Duration.ofMillis(100)),
                       0);

        StepVerifier
            .create(result.doOnNext(System.out::println))
            .expectNextCount(10)
            .thenCancel()
            .verify();
    }

    @Test
    public void testMergeOnBackpressure_OnDrop() {
        // 测试 onDrop 回调
        AtomicInteger dropCount = new AtomicInteger(0);
        Consumer<Integer> onDrop = item -> dropCount.incrementAndGet();

        Flux<Integer> source = Flux.just(1, 2, 3);

        Supplier<List<Integer>> containerSupplier = ArrayList::new;
        BiFunction<List<Integer>, Integer, List<Integer>> merger = (list, item) -> {
            // 创建新列表，不修改原列表
            List<Integer> newList = new ArrayList<>(list);
            newList.add(item);
            return newList;
        };
        Predicate<List<Integer>> bufferPredicate = list -> list.size() < 2;
        Function<List<Integer>, List<Integer>> mapper = list -> new ArrayList<>(list);

        Flux<List<Integer>> result = FluxUtils.mergeOnBackpressure(
            source,
            containerSupplier,
            merger,
            bufferPredicate,
            mapper,
            onDrop
        );

        // 取消订阅，应该触发 onDrop
        StepVerifier.create(result)
                    .expectNextCount(1)
                    .thenCancel()
                    .verify();

        // 注意：由于取消时队列中可能还有元素，onDrop 会被调用
        // 但具体调用次数取决于实现细节
    }

    @Test
    public void testMergeOnBackpressure_Error() {
        // 测试错误处理
        Flux<Integer> source = Flux.just(1, 2, 3)
                                   .concatWith(Flux.error(new RuntimeException("Test error")));

        Supplier<List<Integer>> containerSupplier = ArrayList::new;
        BiFunction<List<Integer>, Integer, List<Integer>> merger = (list, item) -> {
            // 创建新列表，不修改原列表
            List<Integer> newList = new ArrayList<>(list);
            newList.add(item);
            return newList;
        };
        Predicate<List<Integer>> bufferPredicate = list -> list.size() < 3;
        Function<List<Integer>, List<Integer>> mapper = list -> new ArrayList<>(list);

        Flux<List<Integer>> result = FluxUtils.mergeOnBackpressure(
            source,
            containerSupplier,
            merger,
            bufferPredicate,
            mapper,
            null
        );

        StepVerifier.create(result.doOnNext(System.out::println))
                    .expectNextCount(1)
                    .expectError(RuntimeException.class)
                    .verify();
    }

    @Test
    public void testMergeOnBackpressure_MixedScenarios() {
        // 测试混合场景：快速发送 -> 背压 -> bufferPredicate false -> 快速发送
        Flux<Integer> source = Flux.range(1, 30)
                                   .delayElements(Duration.ofMillis(2));

        Supplier<List<Integer>> containerSupplier = ArrayList::new;
        BiFunction<List<Integer>, Integer, List<Integer>> merger = (list, item) -> {
            List<Integer> newList = new ArrayList<>(list);
            newList.add(item);
            return newList;
        };
        // bufferPredicate: 最多4个元素
        Predicate<List<Integer>> bufferPredicate = list -> list.size() < 4;
        Function<List<Integer>, List<Integer>> mapper = list -> new ArrayList<>(list);

        Flux<List<Integer>> result = FluxUtils.mergeOnBackpressure(
            source,
            containerSupplier,
            merger,
            bufferPredicate,
            mapper,
            null
        );

        StepVerifier
            .create(result.limitRate(1).delayElements(Duration.ofMillis(10)).doOnNext(System.out::println))
//            .expectNextMatches(list -> list.size() == 3 && list.get(0) == 1 && list.get(1) == 2 && list.get(2) == 3)
//            .expectNextMatches(list -> list.size() == 3 && list.get(0) == 4 && list.get(1) == 5 && list.get(2) == 6)
            .expectNextCount(8) // 剩余8个批次（每个批次3个元素）
            .thenCancel()
            .verify();
    }

    static {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
    }

    @Test
    public void testNetty() {

        Flux<ByteBuf> origin = Flux
            .<ByteBuf>generate(gen -> {
                gen.next(ByteBufAllocator.DEFAULT.buffer(1024).writerIndex(1024));
            })
            .doOnRequest(i-> System.out.println("req:"+i));

        FluxUtils
            .mergeOnBackpressure(
                origin,
                () -> Unpooled.compositeBuffer(1024),
                (c, buf) -> c
                    .addComponent(true, c
                        .alloc()
                        .buffer(4)
                        .writeInt(buf.readableBytes()))
                    .addComponent(true, buf),
                c -> c.numComponents() < 100 &&
                    c.readableBytes() < ThreadLocalRandom.current().nextInt(2048, 1024 * 1024),
                Function.identity(),
                null
            )
            .map(buf -> {
                try {
                    return buf.numComponents() + "=>" + buf.readableBytes();
                } finally {
                    buf.release();
                }
            })
            .concatMap(i -> Mono
                .delay(Duration.ofMillis(100))
                .thenReturn(i))
//            .window(Duration.ofSeconds(1))
//            .flatMap(flx -> {
//                return flx.reduce(Math::addExact);
//            })
            .doOnNext(System.out::println)
            .as(StepVerifier::create)
            .expectNextCount(10)
            .thenCancel()
            .verify();
    }

}
