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
 *    .getObject(ObjectSpec.parse("test@device:manager@user"))
 *    .flatMap(manager-> manager.properties().get("email"))
 *    .flatMap(email-> sendEmail(email))
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
     * @param objectType 成员类型 {@link ObjectType#getId()}
     * @param objectId   数据ID
     * @return 成员
     */
    Mono<RelationObject> getObject(String objectType,
                                   String objectId);

    /**
     * 获取指定类型的多个对象信息,如: 获取一个设备,然后获取和该设备有关系的信息
     *
     * @param objectType 成员类型 {@link ObjectType#getId()}
     * @param objectIds   数据ID
     * @return 成员
     */
    Flux<RelationObject> getObjects(String objectType,
                                    Collection<String> objectIds);

    /**
     * 根据对象关系描述来获取对象
     *
     * @param spec 对象描述
     * @return void
     * @see ObjectSpec
     */
    default Flux<RelationObject> getObjects(ObjectSpec spec) {
        Flux<RelationObject> first = getObject(spec.getObjectType(), spec.getObjectId()).flux();
        RelationSpec rel = spec.getRelated();
        while (rel != null) {
            RelationSpec fRel = rel;
            first = first
                    .flatMap(obj -> obj
                            .relations(fRel.isReverse())
                            .get(fRel.getObjectType(), fRel.getRelation()));
            rel = fRel.getNext();
        }
        return first;
    }

}
