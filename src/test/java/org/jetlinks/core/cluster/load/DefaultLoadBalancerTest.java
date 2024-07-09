package org.jetlinks.core.cluster.load;

import org.hswebframework.web.id.IDGenerator;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DefaultLoadBalancerTest {


    @Test
    public void test() {
        DefaultLoadBalancer<String> balancer = new DefaultLoadBalancer<>();
        for (int i = 0; i < 5; i++) {
            balancer.register("iot-service:880" + i);
        }
        Map<String, AtomicInteger> count = new LinkedHashMap<>();

        for (int i = 0; i < 100_0000; i++) {
            count.computeIfAbsent(
                balancer.choose(IDGenerator.RANDOM.generate()),
                ignore -> new AtomicInteger()
            ).incrementAndGet();
        }

        System.out.println(count);
    }
}