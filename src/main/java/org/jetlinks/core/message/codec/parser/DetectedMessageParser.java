package org.jetlinks.core.message.codec.parser;

import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MessageParser;
import reactor.core.Disposable;
import reactor.core.Disposables;

import java.util.List;

public class DetectedMessageParser implements MessageParser {

    final Disposable.Swap delegate = Disposables.swap();

    public DetectedMessageParser(MessageParser parser) {
        update(parser);
    }

    @Override
    public List<? extends EncodedMessage> handle(EncodedMessage message) {
        if (delegate.get() instanceof MessageParser parser) {
            return parser.handle(message);
        }
        return List.of();
    }


    public void update(MessageParser parser) {
        delegate.update(parser);
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }
}
