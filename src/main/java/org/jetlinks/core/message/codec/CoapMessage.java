package org.jetlinks.core.message.codec;

import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionNumberRegistry;

import javax.annotation.Nonnull;
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

}
