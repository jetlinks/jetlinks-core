package org.jetlinks.core.things;

import org.jetlinks.core.Wrapper;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.things.relation.PropertyOperation;
import org.jetlinks.core.things.relation.RelationOperation;
import reactor.core.publisher.Mono;

/**
 * 某一稳定 type/id 对象的轻量操作句柄，不要求 Thing 的配置和注册生命周期。
 * 取得句柄不证明对象存在或已授权。实现不得捕获一次调用的认证、观测或业务快照；
 * 读取与响应式命令在每次订阅时使用当前上下文，不主动订阅、阻塞或重试。
 *
 * @since 1.3.3
 * @see ThingMetadata
 * @see org.jetlinks.core.things.relation.RelationObject
 */
public interface ObjectOperations extends Wrapper {

    /** @return 不随句柄存续改变的对象类型，不是产品/模板模型 ID */
    String getType();

    /** @return 来源稳定实例 ID，不是关联目标 ID */
    String getId();

    /**
     * 按订阅解析实例适用模型，不修改来源共享模型。
     *
     * @return 模型；空仅表示未提供模型，解析/服务失败必须保留
     */
    Mono<? extends ThingMetadata> getMetadata();

    /** @return 非空属性句柄；取得句柄不执行 I/O，读取沿用来源路径及空值契约 */
    PropertyOperation properties();

    /**
     * @param reverse false 导航规范出边，true 导航入边
     * @return 非空关系句柄，实际读取返回另一端对象；不支持能力不能冒充空结果
     */
    RelationOperation relations(boolean reverse);

    /** @return 规范出边的操作句柄 */
    default RelationOperation relations() {
        return relations(false);
    }

    /**
     * 同步取得非空命令句柄；功能发现不授予执行权限。
     * 默认没有命令，创建/执行明确失败；来源可返回绑定实例的原 CommandSupport。
     *
     * @return 原命令能力，不执行 I/O，不增加自动重试
     */
    default CommandSupport commands() {
        return UnsupportedObjectCommands.INSTANCE;
    }
}
