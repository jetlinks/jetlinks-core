package org.jetlinks.core.things;

import org.jetlinks.core.config.ConfigKey;

public interface ThingsConfigKeys {

    ConfigKey<Long> version = ConfigKey.of("version", "版本");
    ConfigKey<String> metadata = ConfigKey.of("metadata", "物模型");
    ConfigKey<Long> firstPropertyTime = ConfigKey.of("firstProperty", "首次上报属性的时间");
    ConfigKey<Long> lastMetadataTimeKey = ConfigKey.of("lst_metadata_time","最后修改物模型的时间");

    ConfigKey<String> templateId = ConfigKey.of("templateId", "模版ID");
    ConfigKey<String> name = ConfigKey.of("name", "名称");
}
