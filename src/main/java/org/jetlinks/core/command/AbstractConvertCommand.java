package org.jetlinks.core.command;

import java.util.function.Function;

public abstract class AbstractConvertCommand<Response, Self extends AbstractConvertCommand<Response, Self>>
    extends AbstractCommand<Response, Self> {

    private transient Function<Object, ?> converter;

    public final Self withConverter(Function<Object, ?> converter) {
        this.converter = converter;
        return castSelf();
    }

    @Override
    public final Object createResponseData(Object value) {
        return converter == null
            ? super.createResponseData(value)
            : converter.apply(value);
    }

}
