package org.jetlinks.core.things;

import org.jetlinks.core.config.ConfigKey;
import org.springframework.core.ResolvableType;

import java.util.Map;

public interface ThingsConfigKeys {

    ConfigKey<Long> version = ConfigKey.of("version", "版本");
    ConfigKey<String> metadata = ConfigKey.of("metadata", "物模型");
    ConfigKey<String> type = ConfigKey.of("type", "物类型");
    ConfigKey<Long> firstPropertyTime = ConfigKey.of("firstProperty", "首次上报属性的时间");
    ConfigKey<Long> lastMetadataTimeKey = ConfigKey.of("lst_metadata_time", "最后修改物模型的时间");

    ConfigKey<String> templateId = ConfigKey.of("templateId", "模版ID");
    ConfigKey<String> name = ConfigKey.of("name", "名称");


    ConfigKey<Map<String, Map<String, String>>> i18nMessages =
        ConfigKey.of("i18nMessages", "国际化消息", ResolvableType
            .forClassWithGenerics(Map.class,
                                  ResolvableType.forClass(String.class),
                                  ResolvableType.forClassWithGenerics(Map.class, String.class, String.class)).getType());

}
