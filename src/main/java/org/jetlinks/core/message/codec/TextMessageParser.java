package org.jetlinks.core.message.codec;

import io.vavr.CheckedConsumer;
import io.vavr.CheckedRunnable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * 文本消息解析
 * <p>
 * Start
 * Headers
 * <p>
 * Body
 */
@AllArgsConstructor(staticName = "of")
public class TextMessageParser {

    private final CheckedConsumer<String> startConsumer;

    private final BiConsumer<String, String> headerConsumer;

    private final CheckedConsumer<Payload> bodyConsumer;

    private final CheckedRunnable noBodyConsumer;

    @SneakyThrows
    public void parse(String text) {
        String[] lines = text.trim().split("[\n]");

        int lineIndex = 0;
        for (String line : lines) {
            line = line.trim();
            if(line.startsWith("//")){
                continue;
            }
            if (StringUtils.isEmpty(line)) {
                if (lineIndex > 0) {
                    break;
                }
                continue;
            }
            if (lineIndex++ == 0) {
                startConsumer.accept(line);
            } else {
                String[] header = line.split("[:]");
                if (header.length > 1) {
                    headerConsumer.accept(header[0].trim(), header[1].trim());
                }
            }
        }
        //body
        if (lineIndex < lines.length) {
            String body = String.join("\n", Arrays.copyOfRange(lines, lineIndex, lines.length)).trim();
            MessagePayloadType type;
            byte[] data;
            if (body.startsWith("0x")) {
                type = MessagePayloadType.BINARY;
                data = Hex.decodeHex(body = body.substring(2));
            } else if (body.startsWith("{") || body.startsWith("[")) {
                type = MessagePayloadType.JSON;
                data = body.getBytes();
            } else {
                type = MessagePayloadType.STRING;
                data = body.getBytes();
            }
            bodyConsumer.accept(new Payload(type, body, data));
        } else {
            noBodyConsumer.run();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Payload {
        private final MessagePayloadType type;

        private final String bodyString;

        private final byte[] body;
    }

}
