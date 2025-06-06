package org.jetlinks.core.things;

import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;

/**
 * 支持 Topic 的物类型
 *
 * @author zhouhao
 * @since 1.1.9
 */
public interface TopicSupport {

    /**
     * 根据物类型,获取topic前缀
     *
     * @param templateId 模版ID
     * @param thingId 物ID
     * @return topic前缀
     */
    String getTopicPrefix(String templateId,String thingId);


    default SeparatedCharSequence getTopicPrefix0(String templateId, String thingId) {
        return SharedPathString.of(getTopicPrefix(templateId,thingId));
    }

}
