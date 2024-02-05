package org.jetlinks.core.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

/**
 * 批量消息
 *
 * @author zhouhao
 * @since 1.2.2
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class BatchMessage extends CommonThingMessage<BatchMessage> {

    private List<Message> messages;

    @Override
    public MessageType getMessageType() {
        return MessageType.BATCH;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        if (messages == null) {
            out.writeInt(0);
        } else {
            out.writeInt(messages.size());
            for (Message message : messages) {
                MessageType.writeExternal(message, out);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        int size = in.readInt();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                messages.add(MessageType.readExternal(in));
            }
        }
    }
}
