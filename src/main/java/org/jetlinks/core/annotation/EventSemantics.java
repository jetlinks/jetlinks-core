package org.jetlinks.core.annotation;

import org.jetlinks.core.metadata.EventKind;
import org.jetlinks.core.metadata.MetadataSemantics;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 声明事件模型或载荷的事实种类，例如温度上报标注 data、巡检完成标注 domain。
 * 复用原模型 expands 编码，不创建监听器或订阅，也不把事件发生解释为业务处理成功。
 *
 * @since 1.3.3
 * @see MetadataSemantics#withEventKind
 */
@Documented
@Target({TYPE, METHOD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands
public @interface EventSemantics {
    @Attr(key = MetadataSemantics.EVENT_KIND_KEY)
    EventKind value();
}
