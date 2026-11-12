package org.jetlinks.core.metadata;

/**
 * 事件所描述的事实种类，与事件 ID、业务状态和投递方式分别声明。
 * 例如设备删除是对象生命周期，组织绑定解除是关系实例变化，温度上报是数据事件。
 * 自定义业务事件使用 domain；消费者不认识的开放词仍保留在原 EventMetadata 中。
 *
 * @since 1.3.3
 * @see EventMetadata
 */
public enum EventKind {
    /** 身份来源确认对象已创建、保存、修改或删除；离线和解绑不表示对象删除。 */
    objectLifecycle,
    /** 一种关系的定义变化，例如新增“负责人”关系；不表示已经建立任何绑定。 */
    relationDefinition,
    /** 一条实际关系建立、变更或解除，始终保留规范的源对象与目标对象。 */
    relationLifecycle,
    /** 数据上报、记录产生或来源确认的数据变化；相同数值再次上报仍可产生事件。 */
    data,
    /** 来源声明的业务事实，例如告警产生、巡检完成；含义和授权由来源负责。 */
    domain
}
