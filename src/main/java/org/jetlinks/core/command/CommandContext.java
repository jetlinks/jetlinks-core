package org.jetlinks.core.command;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Optional;

/**
 * 命令上下文,用于在上下文中提供命令支持,用于执行业务回调等场景.
 * <p>
 * 注意: 上下文仅会存在于同一个响应式调用中,请勿缓存上下文进行调用.
 *
 * @author zhouhao
 * @since 1.3
 */
public interface CommandContext {

    /**
     * 获取命令支持,获取不到时返回{@link Mono#empty()}
     *
     * @param name 名称
     * @return 命令支持
     * @see CommandSupport
     */
    Mono<CommandSupport> getCommandSupport(String name);

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

    static Context writeToContext(Context context, CommandContext ctx) {
        CommandContext exist = context.getOrDefault(CommandContext.class, ctx);
        if (exist != null) {
            ctx = ctx.or(exist);
        }
        return context.put(CommandContext.class, ctx);
    }

    static Optional<CommandContext> readFromContext(Context context) {
        return context.getOrEmpty(CommandContext.class);
    }

    static Mono<CommandSupport> currentCommandSupport(String name) {
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

    static Mono<CommandContext> current() {
        return Mono
            .deferContextual(
                ctx -> Mono.justOrEmpty(ctx.getOrEmpty(CommandContext.class)));
    }
}
