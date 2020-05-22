package org.jetlinks.core.topic;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import reactor.test.StepVerifier;

import java.time.Duration;

@Slf4j
public class TopicTest {

    @Test
    public void testSub() {
        Topic<String> root = Topic.createRoot();

        root.append("/device/*").subscribe("testId");

        System.out.println(root);

        root.findTopic("/device/1")
                .map(Topic::getTopic)
                .as(StepVerifier::create)
                .expectNext("/device/*")
                .verifyComplete();
    }

    @Test
    public void testBroadcast() {
        Topic<String> root = Topic.createRoot();
        root.append("/device/1/light/on").subscribe("testId");
        root.append("/device/1/light/off").subscribe("testId");
        root.append("/device/2/light/on").subscribe("testId2");
        root.append("/device/2/light/off").subscribe("testId2");

        root.findTopic("/device/*/*/on")
                .count()
                .as(StepVerifier::create)
                .expectNext(2L)
                .verifyComplete();
    }


    @Test
    public void testBenchmarks() {
        Topic<String> root = Topic.createRoot();

        log.debug("generate topics");
        for (int i = 0; i < 10; i++) {
            Topic<String> product = root.append("/device/" + i);
            for (int i1 = 0; i1 < 10000; i1++) {
                Topic<String> device = product.append("/" + i1);
                device.append("/message/property/read");
                device.append("/message/property/write");
                device.append("/message/property/report");
                device.append("/message/function/invoke");
                device.append("/message/function/invoke/reply");
                device.append("/message/event");
                device.append("/online");
                device.append("/offline");
            }
        }
        log.debug("subscribers:{}", root.getTotalSubscriber());
        log.debug("topics:{}", root.getTotalTopic());

        {
            Duration duration = root.findTopic("/device/1/2/message/property/read")
                    .map(Topic::getTopic)
                    .count()
                    .as(StepVerifier::create)
                    .expectNext(1L)
                    .verifyComplete();
            log.debug("find 1 use time:{}ms", duration.toMillis());
        }

        {
            Duration duration = root.findTopic("/device/1/*/message/property/read")
                    .map(Topic::getTopic)
                    .count()
                    .as(StepVerifier::create)
                    .expectNext(10000L)
                    .verifyComplete();
            log.debug("find device use time:{}ms", duration.toMillis());
        }

        {
            Duration duration = root.findTopic("/device/**")
                    .map(Topic::getTopic)
                    .count()
                    .as(StepVerifier::create)
                    .expectNext(root.getTotalTopic())
                    .verifyComplete();
            log.debug("find all use time:{}ms", duration.toMillis());
        }
    }
}