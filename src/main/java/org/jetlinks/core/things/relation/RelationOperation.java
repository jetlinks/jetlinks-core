package org.jetlinks.core.things.relation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * 关系操作接口
 *
 * @author zhouhao
 * @since  1.2
 * @see RelationObject
 * @see RelatedObject
 */
public interface RelationOperation {

    /**
     * 保存关系
     *
     * @param type     对象类型
     * @param relation 关系
     * @param targetId 目标ID
     * @return 关系对象
     */
    Flux<RelatedObject> save(String type,
                             String relation,
                             Collection<String> targetId);

    /**
     * 保存关系
     *
     * @param type     对象类型
     * @param relation 关系
     * @param targetId 目标ID
     * @return 关系对象
     */
    Mono<RelatedObject> save(String type,
                             String relation,
                             String targetId);

    /**
     * 获取指定类型的目标关系对象,返回以目标对象为主体的关系对象信息.
     * <p>
     *  <ul>
     * <li>type: 对象类型为用户(user)</li>
     * <li>relation: 关系为管理员(manager)</li>
     * <li>targetId: 目标用户id 1</li>
     *  </ul>
     *  获取和当前对象为管理员关系(manager)的用户1(user id为1)的关系信息.
     * <p>
     *  如果返回{@link Mono#empty()}说明不存在关联关系.
     * <p>
     *  如果存在关系则返回:
     *
     *  <pre>{@code
     *  RelatedObject{
     *       relation: "manager",
     *       type: "user"
     *       id: "1"
     *  }
     *  }</pre>
     *
     * @param type     对象类型
     * @param relation 关系标识
     * @param targetId 目标对象
     * @return 关系对象
     */
    Mono<RelatedObject> get(String type,
                            String relation,
                            String targetId);

    /**
     * @see RelationOperation#get(String, String, String)
     */
    Flux<RelatedObject> get(String type,
                            String relation,
                            String... targetId);

    /**
     * @see RelationOperation#get(String, String, String)
     */
    Flux<RelatedObject> get(String type,
                            String relation,
                            Collection<String> targetId);

    /**
     * 获取和当前对象的关系对象
     *
     * @see RelationOperation#get(String, String, String)
     */
    Flux<RelatedObject> get(String type);

    /**
     * 获取和当前对象的关系对象
     *
     * @see RelationOperation#get(String, String, String)
     */
    default Flux<RelatedObject> getAll() {
        return get(null);
    }


}
