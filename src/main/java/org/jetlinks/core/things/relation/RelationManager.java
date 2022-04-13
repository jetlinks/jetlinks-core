package org.jetlinks.core.things.relation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * 关系管理器
 *
 * <ul>
 *     <li>对象类型(ObjectType): 支持建立关系的某一类数据,比如: 用户,角色,部门,设备等</li>
 *     <li>关系对象(RelationObject): 某一成员类型对应的具体的数据,如: 具体的某个用户,设备等</li>
 *     <li>关系(Relation): 对关系的定义,如: 负责人,创建者</li>
 *     <li>和某个对象已建立关系的对象(RelatedObject): 已经和某个成员建立起关系的另外一个成员</li>
 * </ul>
 * <p>
 * <pre>{@code
 *   manager
 *   //获取id为test的设备
 *   .getObject("device","test")
 *   //获取上述设备的管理员用户信息
 *   .flatMap(device-> device.relations().get("user","manager"))
 *   //获取上述用户信息的email信息
 *   .flatMap(manager-> manager.properties().get("email"))
 *   //发送邮件
 *   .flatMap(email-> sendEmail(email))
 *   ...;
 *
 *   //等同于
 *   manager
 *    .evalProperty("test@device:manager@user.email")
 *    .flatMap(prop-> sendEmail(prop.getValue()))
 *    ...
 *
 * }</pre>
 *
 * @author zhouhao
 * @since 1.2
 */
public interface RelationManager {

    /**
     * 获取单个对象类型
     *
     * @param typeId 类型ID
     * @return 对象类型
     */
    Mono<ObjectType> getObjectType(String typeId);

    /**
     * 获取全部对象类型
     *
     * @return 对象类型
     */
    Flux<ObjectType> getObjectTypes();

    /**
     * 获取指定类型的对象信息,如: 获取一个设备,然后获取和该设备有关系的信息
     *
     * @param memberType 成员类型 {@link ObjectType#getId()}
     * @param memberId   数据ID
     * @return 成员
     */
    Mono<RelationObject> getObject(String memberType,
                                   String memberId);

    /**
     * 获取指定类型的多个对象信息,如: 获取一个设备,然后获取和该设备有关系的信息
     *
     * @param memberType 成员类型 {@link ObjectType#getId()}
     * @param memberId   数据ID
     * @return 成员
     */
    Flux<RelationObject> getObjects(String memberType,
                                    Collection<String> memberId);

    /**
     * 根据表达式获取对象属性,表达式格式: {对象ID}@{类型}:{关系}@{类型}.{属性}
     * <pre>{@code
     *  //id为test的设备的管理员的邮箱属性
     *  test@device:manager@user.email
     * }</pre>
     *
     * @param expression 表达式
     * @return 属性, 如果属性或者关系不存在则返回 {@link Flux#empty()}
     */
    Flux<ObjectProperty> evalProperty(String expression);

    /**
     * 根据表达式获取对象关系,表达式格式: {对象ID}@{类型}:{关系}@{类型}
     * <pre>{@code
     *  //id为test的设备的管理员的邮箱属性
     *  test@device:manager@user.email
     * }</pre>
     *
     * @param expression 表达式
     * @return 属性, 如果属性或者关系不存在则返回 {@link Flux#empty()}
     */
    Flux<RelationObject> evalObject(String expression);

}
