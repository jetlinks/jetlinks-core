package org.jetlinks.core.command;

import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.utils.MetadataUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于注解{@link Schema}的命令元数据解析器.
 * <p>
 * 1、在命令类上注解{@link Schema}.
 * <p>
 * 2、在命令参数`getter`方法上注解{@link Schema}.
 *
 * <pre>{@code
 *
 *  @Schema(title="自定义命令",description="命令描述")
 *  public class CustomCommand extends AbstractCommand<Mono<String>, CustomCommand> {
 *
 *      @Schema(title="参数1",description="参数1描述")
 *      public String getArg0(){
 *          return getOrNull("arg0",String.class);
 *      }
 *
 *  }
 *
 * }</pre>
 *
 * @author zhouhao
 * @since 1.2.2
 */
@AllArgsConstructor
public class CommandMetadataResolver {

    /**
     * <pre>
     * 解析传入对象的属性模型
     * 1. 当传入为{@link Command}时，尝试解析其内部类{@code InputSpec}.
     * 2. 当传入为{@link AbstractCommand}时，解析其所有{@code getxxx()}方法为属性模型
     * 3. 当传入对象为属性类时，解析其所有携带{@link Schema}的属性为属性模型
     * </pre>
     *
     * @param type 解析对象
     * @return DataType
     */
    public static List<PropertyMetadata> resolveInputs(ResolvableType type) {
        Class<?> clazz = type.toClass();
        if (Command.class.isAssignableFrom(clazz)) {
            try {
                //尝试获取内部类的InputSpec
                Class<?> inputSpec = clazz
                    .getClassLoader()
                    .loadClass(clazz.getName() + "$InputSpec");
                return resolveInputs(ResolvableType.forClass(inputSpec));
            } catch (ClassNotFoundException ignore) {

            }
            if (AbstractCommand.class.isAssignableFrom(clazz)) {
                List<PropertyMetadata> inputs = new ArrayList<>();
                ReflectionUtils.doWithMethods(clazz, method -> {
                    PropertyMetadata prop = tryResolveProperty(clazz, method);
                    if (prop != null) {
                        inputs.add(prop);
                    }
                });
                return inputs;
            }
        }
        ObjectType objectType = (ObjectType) MetadataUtils.parseType(type);
        return objectType.getProperties();
    }

    /**
     * <pre>
     * 解析传入对象为{@code DataType}
     * 1.当传入对象为{@link Command}时，解析其返回泛型为DataType
     * 2.当传入对象为属性类时，解析其为{@link ObjectType},所有携带{@link Schema}的属性为属性模型
     * </pre>
     *
     * @param type 解析对象
     * @return DataType
     */
    public static DataType resolveOutput(ResolvableType type) {
        Class<?> clazz = type.toClass();
        if (Command.class.isAssignableFrom(clazz)) {
            return MetadataUtils.parseType(
                CommandUtils.getCommandResponseDataType(type.toClass())
            );
        } else {
            return MetadataUtils.parseType(type);
        }

    }

    public static FunctionMetadata resolve(Class<?> commandClazz) {
        return resolve(ResolvableType.forClass(commandClazz));
    }

    public static FunctionMetadata resolve(Class<?> commandClazz, Class<?> outputClazz) {
        return resolve(ResolvableType.forClass(commandClazz),
                       ResolvableType.forClass(outputClazz));
    }

    public static FunctionMetadata resolve(ResolvableType commandClazz) {
        return resolve(commandClazz, commandClazz);
    }

    public static FunctionMetadata resolve(ResolvableType commandClazz, ResolvableType outClazz) {
        SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
        Class<?> clazz = commandClazz.toClass();

        metadata.setId(CommandUtils.getCommandIdByType(clazz));
        Schema schema = AnnotationUtils.findAnnotation(clazz, Schema.class);
        if (schema != null) {
            metadata.setName(StringUtils.hasText(schema.title()) ? schema.title() : schema.description());
            metadata.setDescription(schema.description());
        } else {
            metadata.setName(metadata.getId());
        }
        metadata.setInputs(resolveInputs(commandClazz));
        metadata.setOutput(resolveOutput(outClazz));
        return metadata;
    }


    static PropertyMetadata tryResolveProperty(Class<?> clazz, Method method) {
        Schema schema = AnnotationUtils.findAnnotation(method, Schema.class);
        if (null == schema) {
            return null;
        }
        if (method.getReturnType() == Void.class || method.getParameterCount() != 0) {
            return null;
        }
        String name;
        if (StringUtils.hasText(schema.name())) {
            name = schema.name();
        } else {
            String methodName = method.getName();
            int nameIndex = 0;
            if (methodName.startsWith("get")) {
                nameIndex = 3;
            }
            char[] propertyName = methodName.substring(nameIndex).toCharArray();
            propertyName[0] = Character.toLowerCase(propertyName[0]);
            name = new String(propertyName);
        }

        SimplePropertyMetadata prop = new SimplePropertyMetadata();
        prop.setId(name);
        prop.setDescription(schema.description());
        prop.setName(StringUtils.hasText(schema.title()) ? schema.title() : prop.getDescription());
        prop.setValueType(MetadataUtils.parseType(ResolvableType.forMethodReturnType(method, clazz)));
        return prop;
    }
}
