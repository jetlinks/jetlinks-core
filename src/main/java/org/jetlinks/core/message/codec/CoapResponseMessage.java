package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.apache.commons.codec.binary.Hex;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionNumberRegistry;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public interface CoapResponseMessage extends EncodedMessage {

    @Nonnull
    CoAP.ResponseCode getCode();

    @Nullable
    List<Option> getOptions();

    @Nonnull
    default Optional<Option> getOption(int number) {
        return Optional.ofNullable(getOptions())
                .flatMap(list ->
                        list.stream()
                                .filter(opt -> opt.getNumber() == number)
                                .findFirst());
    }

    @Nonnull
    default Optional<String> getStringOption(int number) {
        return getOption(number)
                .map(Option::getStringValue);
    }

    @Nonnull
    default Optional<Integer> getIntOption(int number) {
        return getOption(number)
                .map(Option::getIntegerValue);
    }

    default String print(boolean pretty) {

        StringBuilder builder = new StringBuilder()
                .append(getCode().name()).append(" ").append(getCode().toString())
                .append("\n");
        if (!CollectionUtils.isEmpty(getOptions())) {
            for (Option option : getOptions()) {
                builder.append(option).append("\n");
            }

        }
        builder.append("\n");
        ByteBuf byteBuf = getPayload();
        if (getOption(OptionNumberRegistry.CONTENT_FORMAT).isPresent()) {
            builder.append(byteBuf.toString(StandardCharsets.UTF_8));
        } else {
            if (pretty) {
                ByteBufUtil.appendPrettyHexDump(builder, getPayload());
            } else {
                builder.append(Hex.encodeHex(payloadAsBytes()));
            }
        }
        return builder.toString();

    }

}
