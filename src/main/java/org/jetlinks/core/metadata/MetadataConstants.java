package org.jetlinks.core.metadata;

import org.jetlinks.core.config.ConfigKey;

import java.util.Locale;
import java.util.Map;

/**
 * @author gyl
 * @since 2.3
 */
public interface MetadataConstants {

    interface Expand {

        ConfigKey<Map</*local*/String, /*name*/String>> LOCALE_NAME_KEY = ConfigKey.of("localeName", "本地化名称", Map.class);


        /**
         * 获取本地化名称
         * @param metadata 属性模型
         * @param locale 区域
         * @return
         */
        static String getLocaleName(PropertyMetadata metadata, Locale locale) {
            return metadata
                .getExpand(LOCALE_NAME_KEY)
                .map(map -> map.get(locale.getLanguage()))
                .orElse(metadata.getName());
        }

    }


}
