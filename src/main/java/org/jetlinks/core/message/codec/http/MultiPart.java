package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 对文件上传的支持,{@link MediaType#MULTIPART_FORM_DATA}
 *
 * @author zhouhao
 * @since 1.1.7
 */
public interface MultiPart {

    Optional<Part> getPart(String name);

    List<Part> getParts();

    static MultiPart of(Part... parts) {
        return of(Arrays.asList(parts));
    }

    static MultiPart of(List<Part> parts) {
        return new SimpleMultiPart(parts);
    }

    static Mono<MultiPart> parse(HttpHeaders headers, Flux<ByteBuf> body) {
        return MultiPartParser.parser(headers, body);
    }

    List<MediaType> MIME_TYPES = Arrays.asList(
            MediaType.MULTIPART_FORM_DATA, MediaType.MULTIPART_MIXED, MediaType.MULTIPART_RELATED
    );

    static boolean isMultiPart(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        for (MediaType supportedMediaType : MIME_TYPES) {
            if (supportedMediaType.isCompatibleWith(mediaType)) {
                return true;
            }
        }
        return false;
    }

}
