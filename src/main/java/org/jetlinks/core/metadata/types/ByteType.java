package org.jetlinks.core.metadata.types;

import lombok.Generated;
import org.hswebframework.web.i18n.LocaleUtils;

@Generated
public class ByteType extends NumberType<Byte> {
    public static final String ID = "byte";

    public static final ByteType GLOBAL = new ByteType();

    public ByteType() {
    }

    @Override
    @Generated
    public String getId() {
        return ID;
    }

    @Override
    @Generated
    public String getName() {
        return LocaleUtils.resolveMessage("message.metadata.type.byte", LocaleUtils.current(), "字节类型");
    }

    @Override
    protected Byte castNumber(Number number) {
        return number.byteValue();
    }

    @Override
    protected int defaultScale() {
        return 0;
    }
}
