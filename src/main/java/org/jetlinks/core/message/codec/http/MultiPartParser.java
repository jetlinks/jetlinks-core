package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

class MultiPartParser {

    private static final MultipartHttpMessageReader reader;

    private static final NettyDataBufferFactory factory = new NettyDataBufferFactory(UnpooledByteBufAllocator.DEFAULT);

    private static final ResolvableType type = ResolvableType.forType(Part.class);

    static {
        DefaultPartHttpMessageReader messageReader = new DefaultPartHttpMessageReader();
        messageReader.setMaxInMemorySize(-1);
        reader = new MultipartHttpMessageReader(messageReader);
    }

    static Mono<MultiPart> parser(HttpHeaders httpHeaders, Flux<ByteBuf> data) {

        return reader
            .read(type, new MultiPartParserReactiveHttpInputMessage(data, httpHeaders), Collections.emptyMap())
            .flatMapIterable(Map::entrySet)
            .flatMapIterable(Map.Entry::getValue)
            .flatMap(part -> part
                .content()
                .map(buf -> factory.wrap(buf.asByteBuffer()))
                .collectList()
                .filter(CollectionUtils::isNotEmpty)
                .map(factory::join)
                .cast(NettyDataBuffer.class)
                .map(buffer -> convertPart(part, buffer.getNativeBuffer()))
            )
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .map(MultiPart::of);

    }

    private static Part convertPart(org.springframework.http.codec.multipart.Part part, ByteBuf byteBuf) {
        if (part instanceof FilePart) {
            FilePart filePart = ((FilePart) part);
            return org.jetlinks.core.message.codec.http.FilePart.of(
                filePart.name(),
                filePart.filename(),
                filePart.headers(),
                byteBuf
            );
        }
        if (part instanceof org.springframework.http.codec.multipart.FormFieldPart) {
            org.springframework.http.codec.multipart.FormFieldPart formFieldPart = ((org.springframework.http.codec.multipart.FormFieldPart) part);
            return FormFieldPart.of(formFieldPart.name(), formFieldPart.value(), formFieldPart.headers(), byteBuf);
        }
        return new SimplePart(part.name(), part.headers(), byteBuf);
    }

    @AllArgsConstructor
    static class MultiPartParserReactiveHttpInputMessage implements ReactiveHttpInputMessage {
        private final Flux<ByteBuf> data;
        private final HttpHeaders headers;

        @Nonnull
        @Override
        public Flux<DataBuffer> getBody() {
            return data.map(factory::wrap);
        }

        @Override
        @Nonnull
        public HttpHeaders getHeaders() {
            return headers;
        }
    }

}
