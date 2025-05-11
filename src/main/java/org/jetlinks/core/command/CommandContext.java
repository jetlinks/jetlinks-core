package org.jetlinks.core.command;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * 命令上下文,用于在上下文中提供命令支持,用于执行业务回调等场景.
 * <p>
 * 注意: 上下文仅会存在于同一个响应式调用中,请勿缓存上下文进行调用.
 *
 * <pre>{@code
 *
 *   // 服务1
 *   Service1{
 *
 *       Mono<Void> handleConnection(NetConnection connection){
 *
 *          // 构造上下文
 *          CommandContext ctx = createCommand("net_comm",connection);
 *
 *          // 获取命令服务
 *          CommandSupport service= ...;
 *
 *          return service
 *                .execute(new HandleConnectionCommand())
 *                .contextWrite(ctx);
 *       }
 *
 *   }
 *
 *  // 服务2
 *   Service2{
 *
 *       @CommandHandler
 *       Mono<Void> handleCommand(HandleConnectionCommand cmd){
 *
 *        return this
 *             .handleConnection(cmd)
 *             .flatMap(ackData->{
 *               return CommandContext
 *                    .current("net_comm")// 获取上下文中指定的命令支持
 *                    // 执行上下文中的命令,
 *                    .flatMap(support->support
 *                          .execute(new SendCommand().payload(ackData)))
 *            })
 *
 *       }
 *
 *   }
 * }</pre>
 *
 * @author zhouhao
 * @since 1.3
 */
public interface CommandContext extends Function<Context, Context> {

    /**
     * 获取命令支持,获取不到时返回{@link Mono#empty()}
     *
     * @param name 名称
     * @return 命令支持
     * @see CommandSupport
     */
    Mono<CommandSupport> getCommandSupport(String name);

    /**
     * @see Mono#contextWrite(Function)
     * @see reactor.core.publisher.Flux#contextWrite(Function)
     */
    @Override
    default Context apply(Context context) {
        return writeToContext(this, context);
    }

    /**
     * 组合另外一个上下文,当前上下文获取不到命令时,使用另外一个上下文进行获取
     *
     * @param context 另外一个上下文
     * @return CommandContext
     */
    default CommandContext or(CommandContext context) {
        return name -> getCommandSupport(name)
            .switchIfEmpty(context.getCommandSupport(name));
    }

    /**
     * 创建一个固定名称的命令支持上下文
     * @param name 名称
     * @param commandSupport 命令支持
     * @return 上下文
     */
    static CommandContext create(String name, Mono<CommandSupport> commandSupport) {
        return _name -> Objects.equals(name, _name) ? commandSupport : Mono.empty();
    }

    /**
     * 将指定的命令上下文设置到响应式上下文并返回新的响应式上下文，不会影响传入的原始响应式上下文。
     *
     * @param context Context
     * @param ctx     CommandContext
     * @return Context
     */
    static Context writeToContext(CommandContext ctx, Context context) {
        CommandContext exist = context.getOrDefault(CommandContext.class, ctx);
        if (exist != null) {
            ctx = ctx.or(exist);
        }
        return context.put(CommandContext.class, ctx);
    }

    /**
     * 从响应式上下文中获取命令上下文
     *
     * @param context 上下文
     * @return Optional
     */
    static Optional<CommandContext> readFromContext(Context context) {
        return context.getOrEmpty(CommandContext.class);
    }

    /**
     * 从当前响应式上下文中获取指定名称的命令支持,如果没有上下文将返回{@link Mono#empty()}.
     *
     * @param name 命令支持
     * @return Optional
     */
    static Mono<CommandSupport> current(String name) {
        return Mono
            .deferContextual(
                ctx -> {
                    CommandContext context = ctx.getOrDefault(CommandContext.class, null);
                    if (context == null) {
                        return Mono.empty();
                    }
                    return context.getCommandSupport(name);
                });
    }

    /**
     * 从当前响应式上下文中获取上下文支持,如果没有上下文将返回{@link Mono#empty()}.
     *
     * @return Mono
     */
    static Mono<CommandContext> current() {
        return Mono
            .deferContextual(
                ctx -> Mono.justOrEmpty(ctx.getOrEmpty(CommandContext.class)));
    }
}
