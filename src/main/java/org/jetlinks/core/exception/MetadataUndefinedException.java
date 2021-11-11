package org.jetlinks.core.exception;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.exception.I18nSupportException;

@Getter
@Setter
public class MetadataUndefinedException extends I18nSupportException {

    private String deviceId;

    public MetadataUndefinedException(String deviceId){
        super("validation.metadata_undefined",deviceId);
    }
}
