package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.apache.commons.codec.binary.Hex;
import org.eclipse.californium.core.coap.*;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * @author zhouhao
 * @since 1.0
 */
public interface CoapMessage extends EncodedMessage {

    /**
     * request uri path
     * e.g. /device/1/property/report
     *
     * @return path
     */
    @Nonnull
    String getPath();

    @Nonnull
    CoAP.Code getCode();

    @Nonnull
    List<Option> getOptions();

    /**
     * @param number option flag
     * @return option value
     * @see OptionNumberRegistry
     * @see OptionNumberRegistry#CONTENT_FORMAT
     */
    @Nonnull
    default Optional<Option> getOption(int number) {
        return getOptions()
                .stream()
                .filter(opt -> opt.getNumber() == number)
                .findFirst();
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

    default Request createRequest(){
        Request request=new Request(getCode());
        request.setURI(getPath());
        request.setPayload(payloadAsBytes());
        getOptions().forEach(request.getOptions()::addOption);
        return request;
    }

    default String print(boolean pretty) {

        StringBuilder builder = new StringBuilder()
                .append(getCode().name()).append(" ").append(getPath())
                .append("\n");

        for (Option option : getOptions()) {
            builder.append(option).append("\n");
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

    static Option parseOption(String option, String value) {
        boolean valueIsNumber = org.hswebframework.utils.StringUtils.isNumber(option);
        boolean valueIsHex = value.startsWith("0x");
        int num = OptionNumberRegistry.toNumber(option);
        if (num == -1 && valueIsNumber) {
            num = Integer.parseInt(option);
        }
        if (num == OptionNumberRegistry.CONTENT_FORMAT && !valueIsNumber) {
            return new Option(num, MediaTypeRegistry.parse(value));
        } else if (OptionNumberRegistry.getFormatByNr(num) == OptionNumberRegistry.optionFormats.INTEGER) {
            return new Option(num, new BigDecimal(value).longValue());
        } else if (valueIsHex) {
            return new Option(num, ByteBufUtil.decodeHexDump(value.substring(2)));
        } else {
            return new Option(num, value);
        }
    }

}
