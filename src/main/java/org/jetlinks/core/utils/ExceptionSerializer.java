package org.jetlinks.core.utils;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.authorization.token.TokenState;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.I18nSupportException;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.exception.RecursiveCallException;
import org.springframework.util.StringUtils;
import reactor.function.Function3;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;

public class ExceptionSerializer implements SerializeUtils.Serializer {

    public static ExceptionSerializer global = new ExceptionSerializer();

    private static final Map<Object, ExceptionSerializerSupport> supports = new HashMap<>();

    static {
        for (InternalExceptionSerializerSupport value : InternalExceptionSerializerSupport.values()) {
            supports.put((byte) value.code, value);
        }

        supports.put(ValidationException.class, InternalExceptionSerializerSupport.validation);
        supports.put(ValidationException.NoStackTrace.class, InternalExceptionSerializerSupport.validation);

        supports.put(NotFoundException.class, InternalExceptionSerializerSupport.notfound);
        supports.put(NotFoundException.NoStackTrace.class, InternalExceptionSerializerSupport.notfound);


        supports.put(UnAuthorizedException.class, InternalExceptionSerializerSupport.unauthorize);
        supports.put(UnAuthorizedException.NoStackTrace.class, InternalExceptionSerializerSupport.unauthorize);


        supports.put(AccessDenyException.class, InternalExceptionSerializerSupport.accessDenied);
        supports.put(AccessDenyException.NoStackTrace.class, InternalExceptionSerializerSupport.accessDenied);

        supports.put(DeviceOperationException.class, InternalExceptionSerializerSupport.deviceOperation);
        supports.put(DeviceOperationException.NoStackTrace.class, InternalExceptionSerializerSupport.deviceOperation);

        supports.put(RecursiveCallException.class, InternalExceptionSerializerSupport.recursiveCall);


    }

    @Override
    public int getCode() {
        return 0xfd;
    }

    @Override
    public Class<?> getJavaType() {
        return Throwable.class;
    }

    @SneakyThrows
    public static Throwable deserialize(DataInput input, boolean includeStackTrace) {
        byte type = input.readByte();
        ExceptionSerializerSupport support = supports.get(type);
        if (support == null) {
            throw new UnsupportedOperationException("unknown exception type:" + type);
        }
        Throwable err = support.deserialize(input);

        if (includeStackTrace) {
            int len = input.readInt();
            if (len > 0) {
                err.setStackTrace(
                    deserializeStackTrace(new StackTraceElement[len], input)
                );
            }
        }

        return err;
    }


    @SneakyThrows
    public static void serialize(Throwable error, DataOutput dataOutput, boolean includeStackTrace) {
        ExceptionSerializerSupport support = supports
            .get(error.getClass());

        if (support == null) {
            // 先检查具体的异常类型，再检查父类型
            if (error instanceof BusinessException) {
                support = InternalExceptionSerializerSupport.business;
            } else if (error instanceof I18nSupportException) {
                support = InternalExceptionSerializerSupport.i18n;
            } else {
                // 其他不支持的类型
                support = InternalExceptionSerializerSupport.undefined;
            }
        }

        dataOutput.writeByte(support.code());
        support.serialize(error, dataOutput);

        if (includeStackTrace) {
            StackTraceElement[] stack = ExceptionUtils.getMergedStackTrace(error);
            dataOutput.writeInt(stack.length);
            serializeStackTrace(stack, dataOutput);
        }
    }

    @SneakyThrows
    public static void serializeStackTrace(StackTraceElement[] error, DataOutput dataOutput) {
        for (StackTraceElement element : error) {
            dataOutput.writeUTF(element.getClassName());
            dataOutput.writeUTF(element.getMethodName());
            dataOutput.writeUTF(element.getFileName() == null ? "" : element.getFileName());
            dataOutput.writeInt(element.getLineNumber());
        }
    }

    @SneakyThrows
    public static StackTraceElement[] deserializeStackTrace(StackTraceElement[] container, DataInput input) {
        for (int i = 0; i < container.length; i++) {
            String className = input.readUTF();
            String methodName = input.readUTF();
            String fileName = input.readUTF();
            int lineNumber = input.readInt();
            container[i] = new StackTraceElement(className, methodName, StringUtils.hasText(fileName) ? fileName : null, lineNumber);
        }
        return container;
    }

    @Override
    @SneakyThrows
    public Object deserialize(ObjectInput input) {
        return ExceptionSerializer.deserialize((DataInput) input, true);
    }

    @Override
    @SneakyThrows
    public void serialize(Object value, ObjectOutput output) {
        ExceptionSerializer.serialize((Throwable) value, (DataOutput) output, true);
    }

    public interface ExceptionSerializerSupport {

        byte code();

        Throwable deserialize(DataInput input);

        void serialize(Throwable error, DataOutput dataOutput);

    }

    @AllArgsConstructor
    enum InternalExceptionSerializerSupport implements ExceptionSerializerSupport {
        undefined(0x00) {
            @Override
            @SneakyThrows
            public Throwable deserialize(DataInput input) {
                String message = input.readUTF();

                return new BusinessException.NoStackTrace(message);
            }

            @Override
            @SneakyThrows
            public void serialize(Throwable error, DataOutput dataOutput) {
                String msg = error.getLocalizedMessage();
                dataOutput.writeUTF(msg == null ? "" : msg);
            }
        },
        business(0x01) {
            @Override
            @SneakyThrows
            public Throwable deserialize(DataInput input) {
                int status = input.readInt();

                return deserializeI18nBase(
                    input, (msg, code, args) -> new BusinessException.NoStackTrace(msg, status, code, args)
                );
            }

            @Override
            @SneakyThrows
            public void serialize(Throwable error, DataOutput dataOutput) {
                BusinessException exp = ((BusinessException) error);
                dataOutput.writeInt(exp.getStatus());
                serializeI18nBase(exp, dataOutput);

            }
        },
        notfound(0x02) {
            @Override
            @SneakyThrows
            public Throwable deserialize(DataInput input) {
                return deserializeI18nBase(
                    input, (msg, code, args) -> new NotFoundException.NoStackTrace(msg, args)
                );

            }

            @Override
            @SneakyThrows
            public void serialize(Throwable error, DataOutput dataOutput) {
                NotFoundException err = ((NotFoundException) error);
                serializeI18nBase(err, dataOutput);
            }
        },
        i18n(0x03) {
            @Override
            @SneakyThrows
            public Throwable deserialize(DataInput input) {
                return deserializeI18nBase(
                    input, (msg, code, args) -> new I18nSupportException.NoStackTrace(msg, args)
                );

            }

            @Override
            @SneakyThrows
            public void serialize(Throwable error, DataOutput dataOutput) {
                serializeI18nBase(((I18nSupportException) error), dataOutput);
            }
        },
        validation(0x04) {
            @Override
            @SneakyThrows
            public Throwable deserialize(DataInput input) {

                int detailSize = input.readInt();
                List<ValidationException.Detail> details = new ArrayList<>(detailSize);
                for (int i = 0; i < detailSize; i++) {
                    details.add(
                        new ValidationException.Detail(
                            input.readUTF(),
                            input.readUTF(),
                            input.readUTF()
                        )
                    );
                }
                return deserializeI18nBase(
                    input, (msg, code, args) -> new ValidationException.NoStackTrace(msg, details, args)
                );

            }

            @Override
            @SneakyThrows
            public void serialize(Throwable error, DataOutput dataOutput) {
                ValidationException err = ((ValidationException) error);
                List<ValidationException.Detail> details = err.getDetails(LocaleUtils.current());
                dataOutput.writeInt(details.size());
                for (ValidationException.Detail detail : details) {
                    dataOutput.writeUTF(detail.getProperty());
                    dataOutput.writeUTF(detail.getMessage());
                    dataOutput.writeUTF(String.valueOf(detail.getDetail()));
                }
                serializeI18nBase(err, dataOutput);
            }
        },
        unauthorize(0x05) {
            static final TokenState[] states = TokenState.values();

            @Override
            @SneakyThrows
            public Throwable deserialize(DataInput input) {
                byte statusValue = input.readByte();
                TokenState state = statusValue < states.length ? states[statusValue] : TokenState.expired;
                return deserializeI18nBase(
                    input, (msg, code, args) -> new UnAuthorizedException.NoStackTrace(msg, state)
                );

            }

            @Override
            @SneakyThrows
            public void serialize(Throwable error, DataOutput dataOutput) {
                UnAuthorizedException err = ((UnAuthorizedException) error);
                dataOutput.writeByte(err.getState().ordinal());

                serializeI18nBase(err, dataOutput);
            }
        },
        accessDenied(0x06) {
            @Override
            @SneakyThrows
            public Throwable deserialize(DataInput input) {
                return deserializeI18nBase(
                    input, (msg, code, args) -> new AccessDenyException.NoStackTrace(msg)
                );

            }

            @Override
            @SneakyThrows
            public void serialize(Throwable error, DataOutput dataOutput) {
                AccessDenyException err = ((AccessDenyException) error);
                serializeI18nBase(err, dataOutput);
            }
        },
        deviceOperation(0x07) {
            static final ErrorCode[] values = ErrorCode.values();

            @Override
            @SneakyThrows
            public Throwable deserialize(DataInput input) {
                byte code = input.readByte();
                ErrorCode errorCode = code < values.length ? values[code] : ErrorCode.UNKNOWN;
                return new DeviceOperationException.NoStackTrace(errorCode, input.readUTF());

            }

            @Override
            @SneakyThrows
            public void serialize(Throwable error, DataOutput dataOutput) {
                DeviceOperationException err = ((DeviceOperationException) error);
                dataOutput.writeByte(err.getCode().ordinal());
                dataOutput.writeUTF(err.getLocalizedMessage());
            }
        },
        recursiveCall(0x20) {
            @Override
            @SneakyThrows
            public Throwable deserialize(DataInput input) {
                int maxRecursive = input.readInt();
                String operation = input.readUTF();

                return new RecursiveCallException(operation, maxRecursive);
            }

            @Override
            @SneakyThrows
            public void serialize(Throwable error, DataOutput dataOutput) {
                RecursiveCallException err = ((RecursiveCallException) error);
                dataOutput.writeInt(err.getMaxRecursive());
                dataOutput.writeUTF(err.getOperation() == null ? "" : err.getOperation());
            }
        };

        @SneakyThrows
        static void serializeI18nBase(I18nSupportException err, DataOutput output) {
            String msg = err.getLocalizedMessage();
            String code = err.getI18nCode();
            Object[] args = err.getArgs();
            output.writeUTF(msg == null ? "" : msg);
            output.writeUTF(code == null ? "" : code);

            if (args == null) {
                output.writeShort(0);
            } else {
                output.writeShort(args.length);
                for (Object arg : args) {
                    output.writeUTF(String.valueOf(arg));
                }
            }
        }

        @SneakyThrows
        public static <T extends Throwable> T deserializeI18nBase(DataInput input,
                                                                  Function3<String, String, Object[], T> builder) {
            String msg = input.readUTF();
            String code = input.readUTF();
            int argLen = input.readUnsignedShort();
            Object[] args;
            if (argLen > 0) {
                args = new Object[argLen];
                for (int i = 0; i < argLen; i++) {
                    args[i] = input.readUTF();
                }
            } else {
                args = new Object[0];
            }
            return builder.apply(msg, code, args);
        }

        private final int code;

        @Override
        public byte code() {
            return (byte) code;
        }
    }
}
