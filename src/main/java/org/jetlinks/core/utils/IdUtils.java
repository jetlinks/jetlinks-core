package org.jetlinks.core.utils;

import org.hswebframework.web.id.SnowflakeIdGenerator;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class IdUtils {

    private static final SnowflakeIdGenerator generator = SnowflakeIdGenerator.create();

    public static String newUUID() {
        return String.valueOf(generator.nextId());
    }
}
