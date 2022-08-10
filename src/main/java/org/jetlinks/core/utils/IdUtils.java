package org.jetlinks.core.utils;

import org.hswebframework.web.id.IDGenerator;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class IdUtils {

    public static String newUUID() {
        return IDGenerator.SNOW_FLAKE_STRING.generate();
    }
}
