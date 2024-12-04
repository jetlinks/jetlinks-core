package org.jetlinks.core.annotation.command;


import io.swagger.v3.oas.annotations.media.Schema;
import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.command.Command;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.springframework.core.ResolvableType;

import java.lang.annotation.*;
import java.lang.reflect.Type;

/**
 * 标记一个方法为命令处理器,用于对外提供命令实现
 *
 * <pre>{@code
 *
 *  @CommandHandler
 *  public Mono<Void> doSomeThing(DoSomeThingCommand cmd){
 *
 *     /// .... do something
 *  }
 *
 *  @CommandHandler(DoSomeThingCommand.class)
 *  public Mono<Void> doSomeThing(String arg1, int arg2){
 *
 *     /// .... do something
 *  }
 *
 *  @CommandHandler(DoSomeThingCommand.class)
 *  public Mono<Void> doSomeThing(@RequestBody MyEntity cmd){
 *
 *     /// .... do something
 *  }
 *
 * }</pre>
 *
 * @author zhouhao
 * @see Command
 * @see Command#as(Type)
 * @see Command#getOrNull(String, Type)
 * @see org.jetlinks.core.command.CommandSupport
 * @see org.jetlinks.core.command.CommandHandler
 * @since 1.2.3
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface CommandHandler {

    /**
     * 原始命令类型,指定了此只时,如果{@link CommandHandler#id()}为空,则命令ID将根据此类型生成.
     * <p>
     * 未指定时,将以方法名作为命令ID
     *
     * @return 命令类型
     * @see org.jetlinks.core.command.CommandUtils#getCommandIdByType(Class)
     */
    Class<? extends Command> value() default Command.class;

    /**
     * 命令ID
     *
     * @return 命令ID
     * @see Command#getCommandId()
     * @see FunctionMetadata#getId()
     */
    String id() default "";

    /**
     * 命令名称
     *
     * @return 命令ID
     * @see FunctionMetadata#getName()
     */
    String name() default "";

    /**
     * 描述,多个值自动拼接.
     *
     * @return 描述
     * @see FunctionMetadata#getDescription()
     */
    String description() default "";

    /**
     * 输入参数定义,通常用于基于java类来描述命令的输入参数
     *
     * @return Class
     * @see FunctionMetadata#getInputs()
     * @see org.jetlinks.core.command.CommandMetadataResolver#resolveInputs(ResolvableType)
     * @see io.swagger.v3.oas.annotations.media.Schema
     * @see Schema#title()
     * @see Schema#description()
     */
    Class<?> inputSpec() default Void.class;

    /**
     * 输出参数定义
     *
     * @return Class
     * @see FunctionMetadata#getOutput()
     */
    Class<?> outputSpec() default Void.class;

    /**
     * 其他拓展配置
     *
     * @return Attr
     * @see FunctionMetadata#getExpands()
     */
    Attr[] expands() default {};

    /**
     * 是否忽略此此方法,通常用于重写父类方法时,忽略掉命令支持.
     *
     * @return 是否忽略
     */
    boolean ignore() default false;
}
