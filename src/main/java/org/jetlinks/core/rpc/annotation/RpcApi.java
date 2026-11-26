package org.jetlinks.core.rpc.annotation;
import java.lang.annotation.*;
/** 稳定的 RPC 合约名；为空时使用接口完整类名。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RpcApi { String value() default ""; }
