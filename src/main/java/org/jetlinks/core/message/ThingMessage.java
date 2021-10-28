package org.jetlinks.core.message;

/**
 * 物消息
 *
 * @since 1.1.9
 */
public interface ThingMessage extends Message {

    /**
     * @return 物ID
     */
    String getThingId();

}
