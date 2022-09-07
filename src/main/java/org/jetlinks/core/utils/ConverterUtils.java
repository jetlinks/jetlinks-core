package org.jetlinks.core.utils;

import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.util.TypeUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.HeaderKey;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ConverterUtils {


    @SuppressWarnings("all")
    public static <T> T convert(Object value, HeaderKey<T> key) {
        return convert(value, key.getValueType());
    }

    @SuppressWarnings("all")
    public static <T> T convert(Object value, Type type) {
        if (value == null ||
                type == Object.class ||
                (type instanceof Class && ((Class) type).isInstance(value))) {
            return (T) value;
        }

        if (type instanceof Class) {
            return (T) FastBeanCopier.DEFAULT_CONVERT.convert(
                    value, (Class) type, FastBeanCopier.EMPTY_CLASS_ARRAY
            );
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = ((ParameterizedType) type);
            Type rawType = parameterizedType.getRawType();

            if (rawType instanceof Class) {
                Type[] args = parameterizedType.getActualTypeArguments();

                Class[] arg = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Class) {
                        arg[i] = (Class) args[i];
                    }
                }

                return (T) FastBeanCopier.DEFAULT_CONVERT.convert(
                        value, (Class) rawType, arg
                );
            }
        }

        return TypeUtils.cast(value, type, ParserConfig.getGlobalInstance());
    }


}
