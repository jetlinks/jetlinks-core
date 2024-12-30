package org.jetlinks.core.topic;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.utils.TopicUtils;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.io.*;
import java.time.Duration;

@Slf4j
public class TopicTest {


    @Test
    @SneakyThrows
    public void testSer(){
        Topic<String> a = Topic.createRoot();

        Topic<String> p = a.append("/device/1/2/3/4");

        ByteArrayOutputStream out =new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        p.writeTo(dout);
        dout.flush();

        Assert.assertArrayEquals(
            TopicUtils.split("/device/1/2/3/4"),
            Topic.readArray(new DataInputStream(new ByteArrayInputStream(out.toByteArray())))
        );

    }

    @Test
    public void testArray() {
        Topic<String> a = Topic.createRoot();

        Assert.assertArrayEquals(
            TopicUtils.split("/device/1/2/3/4"),
            a.append("/device/1/2/3/4").asStringArray()
        );

    }

    @Test
    public void testEq() {
        Topic<String> a = Topic.createRoot();
        Topic<String> b = Topic.createRoot();

        Assert.assertEquals(a.append("/device/1/2/3/4"),
                            b.append("/device/1/2/3/4"));

        Assert.assertEquals(a.append("/device/1/2/3/4").hashCode(),
                            b.append("/device/1/2/3/4").hashCode());

        Assert.assertNotEquals(a.append("/device/2/1/3/4"),
                               b.append("/device/1/2/3/4"));

        Assert.assertNotEquals(a.append("/device/2/1/3/4").hashCode(),
                               b.append("/device/1/2/3/4").hashCode());


    }

    @Test
    public void testRoot() {
        Topic<String> root = Topic.createRoot();

        root.append("/").subscribe("1");

        root.findTopic("/")
            .flatMapIterable(Topic::getSubscribers)
            .as(StepVerifier::create)
            .expectNext("1")
            .verifyComplete();
    }


    @Test
    public void testPattern4() {
        Topic<String> root = Topic.createRoot();
        root.append("/device/*/*/**").subscribe("1");

        root.findTopic("device/0/message/property/report")
            .filter(topicPart -> topicPart.getSubscribers().size() > 0)
            .doOnNext(System.out::println)
            .map(Topic::getTopic)
            .as(StepVerifier::create)
            .expectNext("/device/*/*/**")
            .verifyComplete()
        ;
    }

    @Test
    public void testPattern5() {
        Topic<String> root = Topic.createRoot();
        root.append("/device/electricity-yd/*/*").subscribe("1");

        root.findTopic("/device/onenetv1/online/")
            .doOnNext(System.out::println)
            .map(Topic::getTopic)
            .count()
            .as(StepVerifier::create)
            .expectNext(0L)
            .verifyComplete();
    }


    @Test
    public void testPattern3() {
        Topic<String> root = Topic.createRoot();
        Topic<String> t = root.append("/device/0/message/property/*/reply");
        System.out.println(t.getTopic());
        t.subscribe("1");
        root.append("/device/0/message/property/report").subscribe("1");

        root.findTopic("/device/0/message/property/report")
            .filter(topicPart -> topicPart.getSubscribers().size() > 0)
            .doOnNext(System.out::println)
            .map(Topic::getTopic)
            .as(StepVerifier::create)
            .expectNext("/device/0/message/property/report")
            .verifyComplete()
        ;
    }

    @Test
    public void testMatch2() {
        Topic<String> root = Topic.createRoot();
        root.append("/device/*/online");

        root.findTopic("/device/0/message/property/report")
            .doOnNext(System.out::println)
            .map(Topic<String>::getTopic)
            .as(StepVerifier::create)
            .expectComplete()
            .verify();
    }

    @Test
    public void testFast() {
        Topic<String> root = Topic.createRoot();

        root.append("/device/0/message");

        root.findTopic("/device/0/message")
            .doOnNext(System.out::println)
            .map(Topic::getTopic)
            .as(StepVerifier::create)
            .expectNext("/device/0/message")
            .verifyComplete()
        ;
        ///device/**/event/*
    }


    @Test
    public void testSimple1() {
        Topic<String> root = Topic.createRoot();

        root.append("/device/0/message/event/**");

        root.findTopic("/device/0/message/event/fire_alarm")
            .doOnNext(System.out::println)
            .map(Topic::getTopic)
            .as(StepVerifier::create)
            .expectNext("/device/0/message/event/**")
            .verifyComplete()
        ;
        ///device/**/event/*
    }

    @Test
    public void testPattern2() {
        Topic<String> root = Topic.createRoot();

        root.append("/device/**/fire_alarm");

        root.findTopic("/device/test001/message/event/fire_alarm")
            .doOnNext(System.out::println)
            .map(Topic::getTopic)
            .as(StepVerifier::create)
            .expectNext("/device/**", "/device/**/fire_alarm")
            .verifyComplete()
        ;
        ///device/**/event/*
    }

    @Test
    public void testPattern() {
        Topic<String> root = Topic.createRoot();

        root.append("/device/**/event/*");

        root.findTopic("/device/test001/message/event/fire_alarm")
            .doOnNext(System.out::println)
            .map(Topic::getTopic)
            .as(StepVerifier::create)
            .expectNext("/device/**", "/device/**/event/*")
            .verifyComplete()
        ;
        ///device/**/event/*
    }

    @Test
    public void testMatch() {
        Topic<String> root = Topic.createRoot();
        root.append("/1/org/2/dev/3");
        root.append("/1/org/2/dev/4");

        root.findTopic("/1/org/**/4")
            .map(Topic::getTopic)
            .as(StepVerifier::create)
            .expectNext("/1/org/2/dev/4")
            .verifyComplete();

        root.findTopic("/1/org/*/*/3")
            .map(Topic::getTopic)
            .as(StepVerifier::create)
            .expectNext("/1/org/2/dev/3")
            .verifyComplete();
    }

    @Test
    public void testSimple() {
        Topic<String> root = Topic.createRoot();

        root.append("/**");

        root.append("/1/*");

        root.append("/1/org/2/dev/3");
        root.append("/1/org/2/dev/4");
//
        root.append("/1/org/*");

        root.append("/1/org/**");

        root.findTopic("/1/org/*/dev/*")
            .doOnNext(System.out::println)
            .subscribe();
        System.out.println();

        root.findTopic("/1/org/2/dev/3")
            .doOnNext(System.out::println)
            .map(Topic::getTopic)
            .as(StepVerifier::create)
            .expectNext("/**", "/1/org/**", "/1/org/2/dev/3")
            .verifyComplete();

        Assert.assertNull(root.getTopic("/1/org/5").orElse(null));
        Assert.assertNull(root.getTopic(TopicUtils.split("/1/org/5")).orElse(null));


    }

    @Test
    public void testSub() {
        Topic<String> root = Topic.createRoot();

        root.append(TopicUtils.split("/device/*")).subscribe("testId");

        System.out.println(root);

        root.findTopic("device/1")
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
    @SneakyThrows
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
            long time = System.currentTimeMillis();
            for (int x = 0; x < 10; x++) {
                Duration duration =
                    Flux.range(0, 10000)
                        .flatMap(i -> root
                            .findTopic("/device/1/" + i + "/message/property/read"))
                        .count()
                        .as(StepVerifier::create)
                        .expectNext(10000L)
                        .verifyComplete();
                log.debug("find 10000 use time:{}ms", duration.toMillis());
            }
            log.debug("find 10*10000 use time:{}ms", System.currentTimeMillis() - time);

        }

        {
            Duration duration = root
                .findTopic("/device/1/*/message/property/read")
                .map(Topic::getTopic)
                .count()
                .as(StepVerifier::create)
                .expectNext(10000L)
                .verifyComplete();
            log.debug("find device use time:{}ms", duration.toMillis());
        }
        {
            Duration duration =
                Flux.range(0, 100000)
                    .flatMap(ignore -> root
                        .findTopic("/device/1/2/message/property/read"))
                    .map(Topic::getTopic)
                    .count()
                    .as(StepVerifier::create)
                    .expectNext(100000L)
                    .verifyComplete();
            log.debug("find 100000 time:{}ms", duration.toMillis());
        }
        {
            Duration duration = root
                .findTopic("/device/**")
                .map(Topic::getTopic)
                .count()
                .as(StepVerifier::create)
                .expectNext(root.getTotalTopic())
                .verifyComplete();
            log.debug("find all use time:{}ms", duration.toMillis());
        }

        root.append("/device/*/*/message/property/read")
            .subscribe("testId");

        root.cleanup();

        log.debug("topics:{}", root.getTotalTopic());

    }
}