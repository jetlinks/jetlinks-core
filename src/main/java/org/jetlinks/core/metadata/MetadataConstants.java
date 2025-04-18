package org.jetlinks.core.metadata;

import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.metadata.expand.LocaleResource;
import org.jetlinks.core.things.ThingsConfigKeys;

import java.util.Locale;

/**
 * @author gyl
 * @since 2.3
 * @see Metadata#getI18nName()
 * @see org.jetlinks.core.utils.MetadataUtils#resolveI18nMessage(Locale, Metadata, String, String)
 * @see ThingsConfigKeys#i18nMessages
 */
@Deprecated
public interface MetadataConstants {

    interface Expand {

        ConfigKey<LocaleResource> LOCALE_RESOURCE_KEY = ConfigKey.of("localeResource", "本地化资源", LocaleResource.class);


        /**
         * 获取本地化名称
         *
         * @param metadata 模型
         * @param locale   区域
         * @return 本地化名称
         */
        static String getLocaleName(Metadata metadata, Locale locale) {
            return metadata
                .getExpand(LOCALE_RESOURCE_KEY)
                .map(res -> res.getResource(locale))
                .orElse(metadata.getName());
        }

    }


}
