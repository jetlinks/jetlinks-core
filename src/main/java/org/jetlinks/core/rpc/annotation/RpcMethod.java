package org.jetlinks.core.rpc.annotation;
import java.lang.annotation.*;
/** 稳定的方法名；fireAndForget 不能作为远端处理 ACK。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RpcMethod {
    String value() default "";
    boolean fireAndForget() default false;
}
