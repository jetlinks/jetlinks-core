package org.jetlinks.core.command;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 命令相关工具类
 *
 * @author zhouhao
 * @see CommandMetadataResolver
 * @since 1.2.2
 */
public class CommandUtils {

    /**
     * 根据java命令类型获取命令ID,如果类名以Command结尾,则舍去Command后缀.如:
     * <p>
     * QueryCommand -> Query
     *
     * @param commandType
     * @return 命令ID
     */
    @SuppressWarnings("all")
    public static String getCommandIdByType(Class<?> commandType) {
        String id = commandType.getSimpleName();
        if (id.endsWith("Command")) {
            id = id.substring(0, id.length() - 7);
        }
        return id;
    }

    /**
     * 转换对象为{@link Flux},当对象是{@link Publisher}时,将使用{@link Flux#from(Publisher)}进行转换,
     * 否则使用{@link Flux#just(Object)}进行包装.
     *
     * @param response response
     * @return Flux
     */
    @SuppressWarnings("all")
    public static Flux<Object> convertResponseToFlux(Object response) {
        if (response instanceof Publisher) {
            return Flux.from(((Publisher) response));
        }
        return response == null ? Flux.empty() : Flux.just(response);
    }

    /**
     * 转换对象为{@link Flux},当对象是{@link Publisher}时,将使用{@link Flux#from(Publisher)}进行转换,
     * 否则使用{@link Flux#just(Object)}进行包装.
     *
     * @param response response
     * @return Flux
     */
    @SuppressWarnings("all")
    public static Flux<Object> convertResponseToFlux(Object response, Command<?> cmd) {
        if (response instanceof Publisher) {
            return Flux.from(((Publisher) response))
                       .mapNotNull(cmd::createResponseData);
        }
        return response == null
            ? Flux.empty()
            : Flux.just(cmd.createResponseData(response));
    }

    /**
     * 转换对象为{@link Mono}
     * <p>当对象时{@link Mono}时,直接返回</p>
     * <p>
     * 当对象是{@link Publisher}时,将使用{@link Flux#collectList()}收集流中所有元素为{@link java.util.List}后返回{@link Mono},
     * 否则使用{@link Mono#justOrEmpty(Object)}进行包装.
     *
     * @param response response
     * @return Flux
     */
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

    /**
     * 转换对象为{@link Mono}
     * <p>当对象时{@link Mono}时,直接返回</p>
     * <p>
     * 当对象是{@link Publisher}时,将使用{@link Flux#collectList()}收集流中所有元素为{@link java.util.List}后返回{@link Mono},
     * 否则使用{@link Mono#justOrEmpty(Object)}进行包装.
     *
     * @param response response
     * @return Flux
     */
    @SuppressWarnings("all")
    public static Mono<Object> convertResponseToMono(Object response, Command<?> cmd) {
        if (response instanceof Mono) {
            return ((Mono<?>) response)
                .map(cmd::createResponseData);
        }
        if (response instanceof Publisher) {
            return Flux
                .from(((Publisher) response))
                .map(cmd::createResponseData)
                .collectList();
        }
        return Mono.justOrEmpty(cmd.createResponseData(response));
    }

    //命令返回类型缓存
    private static final Map<Class<?>, ResolvableType> commandResponseType = new ConcurrentHashMap<>();


    /**
     * 获取命令的返回类型
     *
     * @param cmd 命令类型
     * @return 返回类型
     */
    public static ResolvableType getCommandResponseType(Class<?> cmd) {
        return commandResponseType
            .computeIfAbsent(cmd, clazz -> ResolvableType
                .forClass(Command.class, clazz)
                .getGeneric(0));
    }

    /**
     * 获取命令的返回的数据类型, 使用{@link Publisher}和{@link Collection}包装的返回结果,将返回泛型的类型.
     *
     * @param cmd 命令类型
     * @return 数据类型
     */
    public static ResolvableType getCommandResponseDataType(Class<?> cmd) {

        return getCommandResponseDataType(CommandUtils.getCommandResponseType(cmd));
    }

    public static ResolvableType getCommandResponseDataType(ResolvableType type) {
        Class<?> typeClazz = type.toClass();
        if (Publisher.class.isAssignableFrom(typeClazz) ||
            Collection.class.isAssignableFrom(typeClazz)) {
            return type.getGeneric(0);
        }
        return type;
    }

    /**
     * 根据命令对象获取返回类型
     *
     * @param cmd 命令对象
     * @return 返回类型
     * @see CommandUtils#getCommandResponseType(Class)
     */
    public static ResolvableType getCommandResponseType(Command<?> cmd) {
        return getCommandResponseType(cmd.getClass());
    }

    /**
     * 根据命令对象获取返回的数据类型
     *
     * @param cmd 命令对象
     * @return 返回数据类型
     * @see CommandUtils#getCommandResponseDataType(Class)
     */
    public static ResolvableType getCommandResponseDataType(Command<?> cmd) {
        return getCommandResponseDataType(cmd.getClass());
    }

    /**
     * 根据类型创建一个对象转换器
     *
     * @param type 要转换的类型
     * @param <T>  转换结果对象泛型
     * @return 转换器
     */
    @SuppressWarnings("all")
    public static <T> Function<Object, T> createConverter(ResolvableType type) {
        if (type.isAssignableFrom(Void.class)) {
            return val -> (T) val;
        }
        return value -> (T) convertData(type, value);
    }

    /**
     * 转换数据为指定类型
     *
     * @param type  类型
     * @param value 数据
     * @return 转换后的数据
     */
    public static Object convertData(ResolvableType type, Object value) {
        if (type.isInstance(value) || value == null || type.toClass() == Void.class) {
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

    /**
     * 判断命令响应类型是否为{@link Publisher}
     *
     * @param command 命令对象
     * @return 响应结果是否为Publisher
     */
    public static boolean commandResponsePublisher(Command<?> command) {
        return Publisher.class.isAssignableFrom(getCommandResponseType(command).toClass());
    }

    /**
     * 判断命令响应类型是否为{@link Flux}
     *
     * @param command 命令对象
     * @return 响应结果是否为Flux
     * @see CommandUtils#convertResponseToFlux(Object)
     */
    public static boolean commandResponseFlux(Command<?> command) {
        return Flux.class.isAssignableFrom(getCommandResponseType(command).toClass());
    }

    /**
     * 判断命令响应类型是否为{@link Mono}
     *
     * @param command 命令对象
     * @return 响应结果是否为Mono
     * @see CommandUtils#convertResponseToMono(Object)
     */
    public static boolean commandResponseMono(Command<?> command) {
        return Mono.class.isAssignableFrom(getCommandResponseType(command).toClass());
    }

    /**
     * 根据命令返回类型是否为Flux设置扩展标识
     *
     * @param command  命令对象
     * @param metadata 命令参数
     * @return FunctionMetadata
     */
    public static FunctionMetadata wrapMetadata(Command<?> command, FunctionMetadata metadata) {

        if (metadata.getExpand(CommandConstant.responseFlux).isPresent()) {
            return metadata;
        }

        return metadata.expand(CommandConstant.responseFlux, commandResponseFlux(command));
    }
}
