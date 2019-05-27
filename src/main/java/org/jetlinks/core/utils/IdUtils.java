package org.jetlinks.core.utils;

import java.util.UUID;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class IdUtils {

    public static String newUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
