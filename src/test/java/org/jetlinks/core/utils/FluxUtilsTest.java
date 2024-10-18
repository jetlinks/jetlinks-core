package org.jetlinks.core.utils;

import lombok.SneakyThrows;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

/**
 * @author gyl
 */
public class FluxUtilsTest {


    @Test
    @SneakyThrows
    public void distinct() {
        //1秒发送一次数据（共5次）,4秒去重一次,下游能收到2次数据
        Flux.range(1, 5)
            .delayElements(Duration.ofMillis(10))
            .as(FluxUtils.distinct(i -> 1, Duration.ofMillis(40)))
            .limitRate(1)
            .as(StepVerifier::create)
            .expectNextCount(2)
            .verifyComplete();

        //2秒发送一次数据（共3次）,1秒去重一次,下游能收到3次数据
        Flux.range(1, 3)
            .delayElements(Duration.ofMillis(20))
            .as(FluxUtils.distinct(i -> 1, Duration.ofMillis(10)))
            .as(StepVerifier::create)
            .expectNextCount(3)
            .verifyComplete();

    }
}
