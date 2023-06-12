package org.jetlinks.core.command;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
                        .forClass(Command.class,clazz)
                        .getGeneric(0));
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
