package org.jetlinks.core.message.codec;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.metadata.Feature;

@Getter
@AllArgsConstructor
public enum CodecFeature implements Feature {
    /**
     * 标识协议使支持透传消息,支持透传的协议可以在界面上动态配置解析规则
     */
    transparentCodec("协议使用透传"),

    ;

    private final String name;

    @Override
    public String getId() {
        return name();
    }

}
