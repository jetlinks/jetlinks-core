package org.jetlinks.core.message.property;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.utils.CompositeMap;
import org.jetlinks.core.utils.SerializeUtils;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定义属性相关消息操作接口
 *
 * @author zhouhao
 * @see ReportPropertyMessage
 * @see ReadPropertyMessageReply
 * @see WritePropertyMessageReply
 * @since 1.1.7
 */
public interface PropertyMessage extends Externalizable {

    /**
     * 获取全部属性值,key为物模型中的属性ID,value为属性的值
     *
     * @return 属性值Map
     * @see PropertyMetadata#getId()
     */
    @Nullable
    Map<String, Object> getProperties();

    /**
     * 属性源时间信息,key为物模型中的属性ID,value为该属性的Unix时间戳(毫秒)
     *
     * @return 属性源时间信息
     */
    @Nullable
    Map<String, Long> getPropertySourceTimes();

    /**
     * 属性状态信息,key为物模型中的属性ID,value为该属性的状态
     *
     * @return 属性状态信息
     */
    @Nullable
    Map<String, String> getPropertyStates();

    /**
     * 获取消息的Unix时间戳,单位毫秒
     *
     * @return 时间戳
     * @see System#currentTimeMillis()
     */
    long getTimestamp();

    /**
     * 获取指定属性ID的源时间(通常由设备上报该属性产生的时间)
     *
     * @param property 属性ID
     * @return Optional 属性值源间
     */
    default Optional<Long> getPropertySourceTime(@Nonnull String property) {
        Map<String, Long> sourceTime = getPropertySourceTimes();
        if (CollectionUtils.isEmpty(sourceTime)) {
            return Optional.empty();
        }
        return Optional.ofNullable(sourceTime.get(property));
    }

    /**
     * 获取指定属性的状态
     *
     * @param property 属性ID
     * @return Optional 属性状态
     */
    default Optional<String> getPropertyState(@Nonnull String property) {
        Map<String, String> states = getPropertyStates();
        if (CollectionUtils.isEmpty(states)) {
            return Optional.empty();
        }
        return Optional.ofNullable(states.get(property));
    }

    /**
     * 获取属性值
     *
     * @param property 属性ID
     * @return Optional 属性值
     */
    default Optional<Object> getProperty(String property) {
        return Optional
            .ofNullable(getProperties())
            .map(props -> props.get(property));
    }

    /**
     * 获取完整的属性信息(值({@link Property#getValue()}),时间{@link Property#getTimestamp()},状态{@link Property#getState()})
     *
     * @param property 属性ID
     * @return Optional 属性信息
     * @see Property
     */
    default Optional<Property> getCompleteProperty(String property) {
        return this
            .getProperty(property)
            .map(value -> {
                long ts = getPropertySourceTime(property).orElse(getTimestamp());
                String state = getPropertyState(property).orElse(null);
                return SimplePropertyValue.of(property, value, ts, state);
            });
    }

    /**
     * 获取全部完整的属性信息
     *
     * @return 全部属性信息
     */
    @JsonIgnore
    @JSONField(serialize = false)
    default List<Property> getCompleteProperties() {
        Map<String, Object> properties = getProperties();
        if (CollectionUtils.isEmpty(properties)) {
            return Collections.emptyList();
        }
        List<Property> list = new ArrayList<>(properties.size());
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            long ts = getPropertySourceTime(entry.getKey()).orElse(getTimestamp());
            String state = getPropertyState(entry.getKey()).orElse(null);
            list.add(SimplePropertyValue.of(entry.getKey(), entry.getValue(), ts, state));
        }
        return list;
    }

    PropertyMessage properties(Map<String, Object> properties);

    PropertyMessage propertySourceTimes(Map<String, Long> times);

    PropertyMessage propertyStates(Map<String, String> states);

    /**
     * 合并属性
     *
     * @param properties 属性信息
     * @return 合并后的消息
     */
    default PropertyMessage mergeProperties(Map<String, Object> properties) {
        synchronized (this) {
            Map<String, Object> old = getProperties();
            if (old == null) {
                return properties(properties);
            }
            //直接快速合并
            if (old instanceof HashMap
                || old instanceof ConcurrentHashMap
                || old instanceof TreeMap
                || old instanceof Hashtable) {
                old.putAll(properties);
                return this;
            }
            Map<String, Object> copy = new HashMap<>(old);
            copy.putAll(properties);
            return properties(copy);
        }
    }

    @Override
    default void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        properties(SerializeUtils.readMap(in, Maps::newLinkedHashMapWithExpectedSize));
        propertySourceTimes(SerializeUtils.readMap(in, Maps::newLinkedHashMapWithExpectedSize));
        propertyStates(SerializeUtils.readMap(in, Maps::newLinkedHashMapWithExpectedSize));

    }

    @Override
    default void writeExternal(ObjectOutput out) throws IOException {
        SerializeUtils.writeKeyValue(getProperties(), out);
        SerializeUtils.writeKeyValue(getPropertySourceTimes(), out);
        SerializeUtils.writeKeyValue(getPropertyStates(), out);
    }
}
