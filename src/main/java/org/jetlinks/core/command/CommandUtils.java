package org.jetlinks.core.command;

import org.hswebframework.web.bean.FastBeanCopier;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CommandUtils {

    @SuppressWarnings("all")
    public static String getCommandIdByType(Class<? extends Command> commandType) {
        String id = commandType.getSimpleName();
        if (id.endsWith("Command")) {
            id = id.substring(0, id.length() - 7);
        }
        return id;
    }

    @SuppressWarnings("all")
    public static Flux<Object> convertResponseToFlux(Object response) {
        if (response instanceof Publisher) {
            return Flux.from(((Publisher) response));
        }
        return response == null ? Flux.empty() : Flux.just(response);
    }

    @SuppressWarnings("all")
    public static Mono<Object> convertResponseToMono(Object response) {
        if (response instanceof Mono) {
            return ((Mono) response);
        }
        if (response instanceof Publisher) {
            return Flux
                .from(((Publisher) response))
                .collectList();
        }
        return Mono.justOrEmpty(response);
    }

    private static final Map<Class<?>, ResolvableType> commandResponseType = new ConcurrentHashMap<>();

    public static ResolvableType getCommandResponseType(Command<?> cmd) {
        return commandResponseType
            .computeIfAbsent(cmd.getClass(), clazz -> ResolvableType
                .forClass(Command.class, clazz)
                .getGeneric(0));
    }

    public static ResolvableType getCommandResponseDataType(Command<?> cmd) {
        ResolvableType type = CommandUtils.getCommandResponseType(cmd);
        Class<?> typeClazz = type.toClass();
        if (Publisher.class.isAssignableFrom(typeClazz) ||
            Collection.class.isAssignableFrom(typeClazz)) {
            return type.getGeneric(0);
        }
        return type;
    }

    @SuppressWarnings("all")
    public static <T> Function<Object, T> createConverter(ResolvableType type) {
        if (type.isAssignableFrom(Void.class)) {
            return val -> (T) val;
        }
        return value -> (T) convertData(type, value);
    }

    public static Object convertData(ResolvableType type, Object value) {
        if (type.isInstance(value)) {
            return value;
        }
        ResolvableType[] genType = type.getGenerics();

        Class<?>[] genClazz;
        if (genType.length == 0) {
            genClazz = FastBeanCopier.EMPTY_CLASS_ARRAY;
        } else {
            genClazz = new Class[genType.length];
            for (int i = 0; i < genType.length; i++) {
                genClazz[i] = genType[i].toClass();
            }
        }
        return FastBeanCopier.DEFAULT_CONVERT
            .convert(value, type.toClass(), genClazz);
    }

    public static boolean commandResponsePublisher(Command<?> command) {
        return Publisher.class.isAssignableFrom(getCommandResponseType(command).toClass());
    }

    public static boolean commandResponseFlux(Command<?> command) {
        return Flux.class.isAssignableFrom(getCommandResponseType(command).toClass());
    }

    public static boolean commandResponseMono(Command<?> command) {
        return Mono.class.isAssignableFrom(getCommandResponseType(command).toClass());
    }

}
