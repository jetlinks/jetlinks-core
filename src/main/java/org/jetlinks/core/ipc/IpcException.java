package org.jetlinks.core.ipc;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class IpcException extends RuntimeException {
    static final long serialVersionUID = -3387516993124229948L;

    private final IpcCode code;

    public IpcException(IpcCode code) {
        super(code.name());
        this.code = code;
    }

    public IpcException(IpcCode code, String message) {
        super(message);
        this.code = code;
    }

    public IpcException(IpcCode code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public IpcException(IpcCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
