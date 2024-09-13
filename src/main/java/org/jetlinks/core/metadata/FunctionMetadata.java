package org.jetlinks.core.metadata;

import org.jetlinks.core.config.ConfigKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface FunctionMetadata extends Metadata, Jsonable {

    /**
     * @return 输入参数定义
     */
    @Nonnull
    List<PropertyMetadata> getInputs();

    /**
     * @return 输出类型，为null表示无输出
     */
    @Nullable
    DataType getOutput();

    /**
     * @return 是否异步
     */
    boolean isAsync();

    default FunctionMetadata merge(FunctionMetadata another, MergeOption... option) {
        throw new UnsupportedOperationException("不支持功能物模型合并");
    }

    @Override
    default <T> FunctionMetadata expand(ConfigKey<T> key, T value) {
        Metadata.super.expand(key, value);
        return this;
    }

    @Override
    default FunctionMetadata expand(String key, Object value) {
        Metadata.super.expand(key, value);
        return this;
    }
}
