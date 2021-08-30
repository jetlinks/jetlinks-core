package org.jetlinks.core.exception;

import lombok.Getter;
import org.hswebframework.web.exception.I18nSupportException;

@Getter
public class ProductNotActivatedException extends I18nSupportException {
    private String productId;

    public ProductNotActivatedException(String productId) {
        super("error.product_not_activated", productId);
    }
}
