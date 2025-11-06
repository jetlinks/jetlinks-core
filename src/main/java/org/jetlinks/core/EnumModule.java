package org.jetlinks.core;

import org.hswebframework.web.dict.I18nEnumDict;
import org.hswebframework.web.i18n.LocaleUtils;

public interface EnumModule extends Module, I18nEnumDict<String> {

    @Override
    default String getValue() {
        return name();
    }

    @Override
    default String getId() {
        return name();
    }

    @Override
    default String getName() {
        return getText();
    }

    @Override
    default String getDescription() {
        return LocaleUtils
            .resolveMessage(this.getClass().getCanonicalName() + ".description", "");
    }

    @Override
    String getText();
}
