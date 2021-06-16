package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

public class MultiPartTest {


    @Test
    @SneakyThrows
    public void testParse() {
        String hex = StreamUtils.copyToString(new ClassPathResource("mutilpart.hex").getInputStream(), StandardCharsets.UTF_8);

        ByteBuf parts = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(hex));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("multipart/form-data; boundary=--------------------------852347335260220474743959"));
        headers.setContentLength(parts.writerIndex());


        MultiPart.parse(headers, Flux.just(parts))
                 .flatMapIterable(MultiPart::getParts)
                 .doOnNext(System.out::println)
                 .as(StepVerifier::create)
                 .expectNextCount(2)
                 .verifyComplete();

    }
}