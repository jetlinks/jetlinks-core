package org.jetlinks.core.message;

import org.jetlinks.core.utils.SerializeUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
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

    UpdateTingTagsMessage tags(Map<String, Object> tags);

    @Override
    default MessageType getMessageType() {
        return MessageType.UPDATE_TAG;
    }

    @Override
    default void writeExternal(ObjectOutput out) throws IOException {
        ThingMessage.super.writeExternal(out);
        SerializeUtils.writeObject(getTags(), out);
    }

    @Override
    @SuppressWarnings("all")
    default void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ThingMessage.super.readExternal(in);
        tags((Map<String, Object>) SerializeUtils.readObject(in));
    }
}
