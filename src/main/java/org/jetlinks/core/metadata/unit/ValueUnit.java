package org.jetlinks.core.metadata.unit;

import org.jetlinks.core.metadata.Formattable;
import org.jetlinks.core.metadata.Metadata;

import java.io.Serializable;
import java.util.Map;

/**
 * 值单位
 *
 * @author bsetfeng
 * @author zhouhao
 * @version 1.0
 **/
public interface ValueUnit extends Metadata, Formattable, Serializable {

    String getSymbol();

    @Override
    default Map<String, Object> getExpands() {
        return null;
    }
}
