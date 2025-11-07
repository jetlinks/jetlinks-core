package org.jetlinks.core.command;

import org.springframework.core.ResolvableType;

import java.util.function.Function;

public abstract class AbstractConvertCommand<Response, Self extends AbstractConvertCommand<Response, Self>>
    extends AbstractCommand<Response, Self> {

    private transient Function<Object, ?> converter;

    private transient ResolvableType resolvableType;

    @Override
    public ResolvableType responseType() {
        return resolvableType != null ? resolvableType : super.responseType();
    }

    public Self withResponseType(ResolvableType type) {
        this.resolvableType = type;
        return castSelf();
    }

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
