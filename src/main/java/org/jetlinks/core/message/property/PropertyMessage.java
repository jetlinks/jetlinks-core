package org.jetlinks.core.message.property;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 定义属性相关消息操作接口
 *
 * @author zhouhao
 * @see ReportPropertyMessage
 * @see ReadPropertyMessageReply
 * @see WritePropertyMessageReply
 * @since 1.1.7
 */
public interface PropertyMessage {

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
        return Optional.of(sourceTime.get(property));
    }

    /**
     * 获取指定属性的状态
     *
     * @param property 属性ID
     * @return Optional 属性状态
     */
   default Optional<String> getPropertyState(@Nonnull String property){
       Map<String, String> states = getPropertyStates();
       if (CollectionUtils.isEmpty(states)) {
           return Optional.empty();
       }
       return Optional.of(states.get(property));
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
        return properties
                .entrySet()
                .stream()
                .map(prop -> {
                    long ts = getPropertySourceTime(prop.getKey()).orElse(getTimestamp());
                    String state = getPropertyState(prop.getKey()).orElse(null);
                    return SimplePropertyValue.of(prop.getKey(), prop.getValue(), ts, state);
                })
                .collect(Collectors.toList());
    }
}
