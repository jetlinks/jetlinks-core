package org.jetlinks.core.message;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 更新物标签消息
 *
 * @author zhouhao
 * @since 1.1.9
 */
public interface UpdateTingTagsMessage extends ThingMessage {

    /**
     * key为标签ID，value为标签值
     *
     * @return 标签信息
     */
    @Nullable
    Map<String, Object> getTags();

    @Override
    default MessageType getMessageType() {
        return MessageType.UPDATE_TAG;
    }
}
