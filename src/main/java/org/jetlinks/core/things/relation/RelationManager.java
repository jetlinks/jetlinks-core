package org.jetlinks.core.things.relation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 关系管理器
 *
 * <ul>
 *     <li>成员类型(MemberType): 支持建立关系的某一类数据,比如: 用户,角色,部门,设备等</li>
 *     <li>成员(Member): 某一成员类型对应的具体的数据,如: 具体的某个用户,设备等</li>
 *     <li>关系(Relation): 对关系的定义,如: 负责人,创建者</li>
 *     <li>关系成员(RelationMember): 已经和某个成员建立起关系的另外一个成员</li>
 * </ul>
 *
 * <pre>{@code
 *   manager
 *   //获取id为test的设备
 *   .getMember("device","test")
 *   //获取上述设备的管理员用户信息
 *   .flatMap(member-> member.relations().get("user","manager"))
 *   //获取上述用户信息的email信息
 *   .flatMap(manager-> manager.properties().get("email"))
 *   //发送邮件
 *   .flatMap(email->sendEmail(email))
 *
 * }</pre>
 *
 * @author zhouhao
 * @since 1.2
 */
public interface RelationManager {

    Flux<MemberType> getMemberType(String typeId);

    /**
     * 获取全部成员类型
     *
     * @return 成员类型
     */
    Flux<MemberType> getMemberTypes();

    /**
     * 获取指定类型的成员信息,如: 获取一个设备,然后获取和该设备有关系的信息
     *
     * @param memberType 成员类型 {@link MemberType#getId()}
     * @param memberId   数据ID
     * @return 成员
     */
    Mono<Member> getMember(String memberType,
                           String memberId);

}
